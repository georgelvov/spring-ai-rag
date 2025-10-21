package com.glvov.springairag.ai;

import com.glvov.springairag.dictionary.AiTemplate;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Advisor для обогащения промпта контекстом из VectorStore (RAG).
 * Выполняется перед отправкой запроса в LLM: достаёт документы из векторного стора
 * и добавляет их в prompt.
 */
@Slf4j
@Builder
public class RagAdvisor implements BaseAdvisor {

    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";
    // ключ для хранения обогащённого вопроса
    public static final String ORIGINAL_QUESTION = "ORIGINAL_QUESTION";
    // ключ для хранения оригинального вопроса

    private final VectorStore vectorStore;
    // Источник документов (например, Postgres + pgvector)

    @Getter
    private final int order;
    // Порядок в цепочке Advisor'ов

    private final int topK;
    // Сколько документов извлекать при поиске
    private final double similarityThreshold;
    // Минимальная схожесть документа с запросом (0–1)
//    private final int maxContextChars;
//    // Ограничение длины итогового контекста в символах

    private static final PromptTemplate template = PromptTemplate.builder()
            .template(AiTemplate.TEMPLATE_2.getTemplate())
            // Загружаем шаблон для финального промпта
            .build();

    // Метод вызывается до обращения к LLM
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {

        // Проверяем, нет ли уже assistant сообщений в промпте
        boolean hasAssistantMessages = chatClientRequest.prompt().getUserMessages().stream()
                .anyMatch(msg -> msg.getMessageType() == MessageType.ASSISTANT);

        if (hasAssistantMessages) {
            log.debug("RAG: Пропускаем добавление system message - уже есть assistant сообщения");
            return chatClientRequest;
        }

        // Получаем исходный вопрос пользователя
        String originalUserQuestion = chatClientRequest.prompt().getUserMessage().getText();

        // Берём либо обогащённый вопрос (если уже был создан ExpansionQueryAdvisor),
        // либо используем оригинальный
        String queryToRag = chatClientRequest.context()
                .getOrDefault(ENRICHED_QUESTION, originalUserQuestion)
                .toString();

        log.debug("RAG: original='{}', enriched='{}'", originalUserQuestion, queryToRag);

        // Поиск документов в векторном сторе
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(queryToRag)              // что ищем
                        .topK(topK)                     // сколько документов вернуть
                        .similarityThreshold(similarityThreshold) // минимальный порог схожести
                        .build()
        );

        if (documents.isEmpty()) {
            // Если ничего не найдено, возвращаем запрос без изменений
            log.info("RAG: контекст не найден для '{}'", queryToRag);
            return chatClientRequest;
        }

        // Формируем текстовый контекст из документов
        String llmContext = documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

        // Подставляем найденный контекст и вопрос в шаблон
        String finalUserPrompt = template.render(
                Map.of("context", llmContext, "question", queryToRag)
        );

        log.debug("RAG: добавлен контекст ({} документов, {} символов)",
                documents.size(), llmContext.length());

        // Модифицируем запрос: добавляем контекст и финальный system-message
        return chatClientRequest.mutate()
                .context(ORIGINAL_QUESTION, originalUserQuestion) // кладём оригинальный вопрос в контекст
                .context(ENRICHED_QUESTION, queryToRag)           // кладём обогащённый/финальный вопрос
                .prompt(
                        chatClientRequest.prompt()
                                .augmentSystemMessage(finalUserPrompt) // добавляем RAG-контекст к system-промпту
                )
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }
}
