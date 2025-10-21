package com.glvov.springairag.ai;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Value("${rag.advisor.top-k:3}")
    private int topK;

    // по умолчанию 0.8 → порог схожести
    @Value("${rag.advisor.similarity-threshold:0.8}")
    private double similarityThreshold;


    @Bean
    public RagAdvisor ragCustomAdvisor(VectorStore vectorStore) {
        return RagAdvisor.builder()
                .vectorStore(vectorStore)
                .order(3)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
    }
}