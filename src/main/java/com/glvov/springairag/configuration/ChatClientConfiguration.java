package com.glvov.springairag.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfiguration {

    /**
     * A system prompt is a special message that defines the role, style,
     * and behavior of a language model (LLM).
     * In LLMs, there are three types of messages:
     * <p>
     * system – defines the model's role and behavior rules
     * user – messages from the user
     * assistant – responses from the model
     */
    @Value("classpath:ai/prompts/system.txt")
    private final Resource systemPromptResource;

    @Qualifier("mainChatOptions")
    private final ChatOptions chatOptions;

    private final List<Advisor> advisors;


    @Bean
    @Primary
    public ChatClient mainChatClient(ChatClient.Builder builder) {
        String systemPrompt = new SystemPromptTemplate(systemPromptResource).render();

        return builder
                .defaultOptions(chatOptions)
                .defaultAdvisors(advisors)
                .defaultSystem(systemPrompt)
                .build();
    }
}
