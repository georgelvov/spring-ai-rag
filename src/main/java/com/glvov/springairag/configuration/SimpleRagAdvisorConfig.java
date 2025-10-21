package com.glvov.springairag.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
@RequiredArgsConstructor
public class SimpleRagAdvisorConfig {

    private static final String PROMPT_TEMPLATE = """
            {query}
            
            Контекст:
            ---------------------------
            {question_answer_context}
            ---------------------------
            
            Отвечай только на основе контекста выше.
            Если информации нет в контексте, сообщи, что не можешь ответить
            """;

    private final VectorStore vectorStore;


    //@Bean
    public Advisor getSimpleRagAdvisor() {
        SearchRequest searchRequest = createSearchRequest();

        return QuestionAnswerAdvisor
                .builder(vectorStore)
                .promptTemplate(new PromptTemplate(PROMPT_TEMPLATE))
                .searchRequest(searchRequest)
                .order(3)
                .build();
    }

    private SearchRequest createSearchRequest() {
        return SearchRequest.builder()
                .topK(4)
                .similarityThreshold(0.65)
                .build();
    }
}
