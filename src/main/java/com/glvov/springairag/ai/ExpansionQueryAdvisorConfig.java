package com.glvov.springairag.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExpansionQueryAdvisorConfig {

    @Value("${expansion.advisor.temperature:0.1}")
    private double temperature;

    @Value("${expansion.advisor.top-p:0.4}")
    private double topP;


    @Bean
    public ExpansionQueryAdvisor expansionQueryAdvisor(ChatModel chatModel) {
        return ExpansionQueryAdvisor.builder(chatModel, temperature, topP)
                .order(1) // задаём порядок выполнения в цепочке Advisor’ов (выполняется вторым, если Rag стоит на 0)
                .build();
    }
}
