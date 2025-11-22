package com.glvov.springairag.configuration;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientOptionsConfig {

    @Bean("mainChatOptions")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "ollama")
    public ChatOptions mainChatOllamaOptions() {
        return OllamaOptions.builder()
                .temperature(0.3)
                .topP(0.7)
                .topK(20)
                .repeatPenalty(1.1) // specific ollama property
                .build();
    }

    @Bean("mainChatOptions")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "mistral")
    public ChatOptions mainChatMistralOptions() {
        return MistralAiChatOptions.builder()
                .temperature(0.3)
                .topP(0.7)
                //.topK(20) // mistral llm doesn't accept topK property
                .build();
    }

    @Bean(name = "queryAdvisorChatOptions")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "ollama")
    public ChatOptions queryAdvisorChatOllamaOptions() {
        return OllamaOptions.builder()
                .temperature(0.0)
                .topP(0.1)
                .topK(1)
                .build();
    }

    @Bean(name = "queryAdvisorChatOptions")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "mistral")
    public ChatOptions queryAdvisorChatMistralOptions() {
        return MistralAiChatOptions.builder()
                .temperature(0.0)
                .topP(1.0) // topP must be 1.0 because of 0.0 temperature - mistral in greedy sampling mode
                //.topK(1)  // mistral llm doesn't accept topK property
                .build();
    }
}
