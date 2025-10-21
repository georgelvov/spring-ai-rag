package com.glvov.springairag.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final PostgresChatMemory postgresChatMemory;
    private final ExpansionQueryAdvisor expansionQueryAdvisor;
    private final RagAdvisor ragAdvisor;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        var options = MistralAiChatOptions.builder()
                .temperature(0.3) // creativity level, higher - more creativity from LLM
                .build();

        return builder
                .defaultAdvisors(  // advisors before prompt goes to LLM
                        expansionQueryAdvisor,
                        getChatMemoryAdvisor(2),
                        ragAdvisor,
                        SimpleLoggerAdvisor.builder().order(4).build()
                )
                .defaultOptions(options)
                .build();
    }

    private Advisor getChatMemoryAdvisor(int order) {
        return MessageChatMemoryAdvisor.builder(postgresChatMemory)
                .order(order)
                .build();
    }
}
