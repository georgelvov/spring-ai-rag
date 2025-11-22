package com.glvov.springairag.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

//@Configuration
@RequiredArgsConstructor
public class SimpleRagAdvisorConfig {

    //@Value("classpath:ai/prompts/question_and_answer.txt")
    private final Resource prompt;

    private final VectorStore vectorStore;


    //@Bean
    public Advisor simpleRagAdvisor() {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(4)
                .similarityThreshold(0.65)
                .build();

        return QuestionAnswerAdvisor
                .builder(vectorStore)
                .promptTemplate(new PromptTemplate(prompt))
                .searchRequest(searchRequest)
                .order(3)
                .build();
    }
}
