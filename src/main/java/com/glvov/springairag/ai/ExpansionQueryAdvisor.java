package com.glvov.springairag.ai;

import com.glvov.springairag.dictionary.AiTemplate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.mistralai.MistralAiChatOptions;

import java.util.Map;

@Slf4j
@Builder
public class ExpansionQueryAdvisor implements BaseAdvisor {

    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";
    public static final String ORIGINAL_QUESTION = "ORIGINAL_QUESTION";

    private final ChatClient chatClient;
    private final int order;
    private final double temperature;
    private final double topP; // более разнообразные ответы

    private static final PromptTemplate TEMPLATE = PromptTemplate.builder()
            .template(AiTemplate.TEMPLATE_1.getTemplate())
            .build();

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String originalQuery = chatClientRequest.prompt().getUserMessage().getText();

        log.info("Original query: {}", originalQuery);

        String expandedQuery = expand(originalQuery);

        log.info("ExpandedQuery query: {}", expandedQuery);

        return chatClientRequest.mutate()
                .context(ORIGINAL_QUESTION, originalQuery)
                .context(ENRICHED_QUESTION, expandedQuery)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public static ExpansionQueryAdvisorBuilder builder(ChatModel chatModel, double temperature, double topP) {
        return new ExpansionQueryAdvisorBuilder()
                .chatClient(
                        ChatClient.builder(chatModel)
                                .defaultOptions(MistralAiChatOptions.builder().temperature(temperature).topP(topP).build())
                                .build()
                );
    }


    private String expand(String originalQuery) {
        String renderedPrompt = TEMPLATE.render(Map.of("question", originalQuery));
        return chatClient.prompt()
                .user(renderedPrompt)
                .call()
                .content();
    }
}
