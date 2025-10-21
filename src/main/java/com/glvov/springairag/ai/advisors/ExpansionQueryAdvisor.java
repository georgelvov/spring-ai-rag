package com.glvov.springairag.ai.advisors;

import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.util.StringUtils;

import java.util.Map;

@Builder
public class ExpansionQueryAdvisor implements BaseAdvisor {

    private static final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.builder()
            .template("""
                    Instruction: Расширь поисковый запрос, добавив наиболее релевантные термины.
                    
                    СПЕЦИАЛИЗАЦИЯ ПО SPRING FRAMEWORK:
                    - Жизненный цикл Spring бинов: конструктор → BeanPostProcessor → PostConstruct → прокси → ContextListener
                    - Технологии: Dynamic Proxy, CGLib, reflection, аннотации, XML конфигурация
                    - Компоненты: BeanFactory, ApplicationContext, BeanDefinition, MBean, JMX
                    - Паттерны: dependency injection, AOP, профилирование, перехват методов
                    
                    ПРАВИЛА:
                    1. Сохрани ВСЕ слова из исходного вопроса
                    2. Добавь МАКСИМУМ ПЯТЬ наиболее важных термина
                    3. Выбирай самые специфичные и релевантные слова
                    4. Результат - простой список слов через пробел
                    
                    СТРАТЕГИЯ ВЫБОРА:
                    - Приоритет: специализированные термины
                    - Избегай общих слов
                    - Фокусируйся на ключевых понятиях
                    
                    ПРИМЕРЫ:
                    "что такое спринг" → "что такое спринг фреймворк Java"
                    "как создать файл" → "как создать файл документ программа"
                    
                    Question: {question}
                    Expanded query:
                    """).build();


    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";
    public static final String ORIGINAL_QUESTION = "ORIGINAL_QUESTION";
    public static final String EXPANSION_RATIO = "EXPANSION_RATIO";


    @Getter
    private final int order;

    private ChatClient chatClient;


    public static ExpansionQueryAdvisorBuilder builder(ChatModel chatModel) {
        ChatClient client = ChatClient.builder(chatModel)
                .defaultOptions(
                        OllamaOptions.builder()
                                .temperature(0.0)
                                .topK(1)
                                .topP(0.1)
                                .repeatPenalty(1.0) // default is 1.0, just for visibility
                                .build()
                )
                .build();

        return new ExpansionQueryAdvisorBuilder().chatClient(client);
    }


    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String userQuestion = chatClientRequest.prompt().getUserMessage().getText();
        String enrichedQuestion = chatClient
                .prompt()
                .user(PROMPT_TEMPLATE.render(Map.of("question", userQuestion)))
                .call()
                .content();

        if (!StringUtils.hasText(enrichedQuestion)) {
            return chatClientRequest;
        }

        double ratio = enrichedQuestion.length() / (double) userQuestion.length();

        return chatClientRequest.mutate()
                .context(ENRICHED_QUESTION, enrichedQuestion)
                .context(ORIGINAL_QUESTION, userQuestion) // only for logging purposes
                .context(EXPANSION_RATIO, ratio) // only for logging purposes
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }
}
