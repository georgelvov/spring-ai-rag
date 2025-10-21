package com.glvov.springairag.ai;

import com.glvov.springairag.dictionary.AiRole;
import com.glvov.springairag.model.entity.AiChat;
import com.glvov.springairag.model.entity.AiChatMessage;
import com.glvov.springairag.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.glvov.springairag.utils.ApplicationUtils.streamOfNullable;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostgresChatMemory implements ChatMemory {

    private final AiChatService aiChatService;

    @Value("${max-messages:50}") // todo: create prop
    private int maxMessages;


    @Override
    public void add(String conversationId, List<Message> messages) {
        log.info("Adding {} messages to custom postgres chat memory, conversationId={}", messages.size(), conversationId);

        AiChat aiChat = aiChatService.getChat(UUID.fromString(conversationId));

        for (Message message : messages) {
            AiChatMessage aiChatMessage = new AiChatMessage()
                    .setAiChat(aiChat)
                    .setText(message.getText())
                    .setAiRole(getAiRole(message.getMessageType()));

            aiChat.getAiChatMessages().add(aiChatMessage);
        }

        aiChatService.saveChat(aiChat);
    }

    @Override
    public List<Message> get(String conversationId) {
        log.info("Getting messages by conversationId={}", conversationId);

        AiChat aiChat = aiChatService.getChat(UUID.fromString(conversationId));

        log.info("Found messages: {}", aiChat.getAiChatMessages().size());

        return streamOfNullable(aiChat.getAiChatMessages())
                .skip(Math.max(0, aiChat.getAiChatMessages().size() - maxMessages))
                .map(this::mapToMessage)
                .toList();
    }

    private Message mapToMessage(AiChatMessage aiMessage) {
        return switch (aiMessage.getAiRole()) {
            case USER -> new UserMessage(aiMessage.getText());
            case ASSISTANT -> new AssistantMessage(aiMessage.getText());
            case SYSTEM -> new SystemMessage(aiMessage.getText());
        };
    }

    @Override
    public void clear(String conversationId) {
    }

    // todo: probably use MessageType instead of AiRole
    private AiRole getAiRole(MessageType messageType) {
        return switch (messageType) {
            case USER -> AiRole.USER;
            case ASSISTANT -> AiRole.ASSISTANT;
            case SYSTEM -> AiRole.SYSTEM;
            default -> null;
        };
    }

}
