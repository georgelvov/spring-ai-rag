package com.glvov.springairag.configuration;

import com.glvov.springairag.advisor.ExpansionQueryAdvisor;
import com.glvov.springairag.advisor.RagAdvisor;
import com.glvov.springairag.advisor.RequestLoggerAdvisor;
import com.glvov.springairag.service.PostgresChatMemory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AdvisorsConfig {

    @Getter
    @RequiredArgsConstructor
    public enum AdvisorOrder {
        EXPANSION_QUERY(0),
        CHAT_MEMORY(1),
        LOG_AFTER_CHAT_MEMORY(2),
        RAG(3),
        LOG_AFTER_RAG(4);

        public final int value;
    }

    @Bean
    public List<Advisor> advisors(ExpansionQueryAdvisor expansionQueryAdvisor,
                                  MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                  RagAdvisor ragAdvisor) {
        return List.of(
                expansionQueryAdvisor,
                messageChatMemoryAdvisor,
                requestLoggerAdvisor(),
                ragAdvisor,
                requestResponseLoggerAdvisor()
        );
    }

    @Bean
    public ExpansionQueryAdvisor expansionQueryAdvisor(ChatClient.Builder builder,
                                                       @Qualifier("queryAdvisorChatOptions")
                                                       ChatOptions chatOptions) {
        ChatClient chatClient = builder
                .defaultOptions(chatOptions)
                .build();

        return ExpansionQueryAdvisor
                .builder(chatClient)
                .order(AdvisorOrder.EXPANSION_QUERY.getValue())
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(PostgresChatMemory chatMemory) {
        return MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(AdvisorOrder.CHAT_MEMORY.getValue())
                .build();
    }

    @Bean
    public RagAdvisor ragAdvisor(VectorStore vectorStore) {
        return RagAdvisor
                .build(vectorStore)
                .order(AdvisorOrder.RAG.getValue())
                .rerankEnabled(false)
                .build();
    }

    private RequestLoggerAdvisor requestLoggerAdvisor() {
        return RequestLoggerAdvisor.builder()
                .order(AdvisorOrder.LOG_AFTER_CHAT_MEMORY.getValue())
                .build();
    }

    private SimpleLoggerAdvisor requestResponseLoggerAdvisor() {
        return SimpleLoggerAdvisor.builder()
                .order(AdvisorOrder.LOG_AFTER_RAG.getValue())
                .build();
    }
}
