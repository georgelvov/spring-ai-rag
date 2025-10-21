package com.glvov.springairag.configuration;

import com.glvov.springairag.ai.advisors.ExpansionQueryAdvisor;
import com.glvov.springairag.ai.advisors.RagAdvisor;
import com.glvov.springairag.repository.ChatRepository;
import com.glvov.springairag.services.PostgresChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfiguration {

    /**
     * A system prompt is a special message that defines the role, style, and behavior of a language model (LLM)
     * In LLMs, there are three types of messages:
     * <p>
     * system – defines the model's role and behavior rules
     * user – messages from the user
     * assistant – responses from the model
     */
    private static final String SYSTEM_PROMPT = """
            Ты — Евгений Борисов, Java-разработчик и эксперт по Spring.
            Отвечай от первого лица, кратко и по делу.
            """;

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ChatRepository chatRepository;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        OllamaOptions options = OllamaOptions.builder()
                .temperature(0.3)
                .topP(0.7)
                .topK(20)
                .repeatPenalty(1.1)
                .build();

        return builder
                .defaultOptions(options)
                .defaultSystem(new PromptTemplate(SYSTEM_PROMPT).render())
                .defaultAdvisors(
                        ExpansionQueryAdvisor.builder(chatModel).order(0).build(),
                        MessageChatMemoryAdvisor.builder(getChatMemory()).order(1).build(),
                        SimpleLoggerAdvisor.builder().order(2).build(),
                        RagAdvisor.build(vectorStore).order(3).build(),
                        SimpleLoggerAdvisor.builder().order(4).build()
                )
                .build();
    }

    private ChatMemory getChatMemory() {
        return PostgresChatMemory.builder()
                .maxMessages(8)
                .chatMemoryRepository(chatRepository)
                .build();
    }
}
