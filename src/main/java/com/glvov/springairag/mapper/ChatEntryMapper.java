package com.glvov.springairag.mapper;

import com.glvov.springairag.model.ChatEntry;
import com.glvov.springairag.model.Role;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatEntryMapper {

    public ChatEntry toChatEntry(Message message) {
        return ChatEntry.builder()
                .role(Role.fromString(message.getMessageType().getValue()))
                .content(message.getText())
                .build();
    }

    public Message toMessage(ChatEntry chatEntry) {
        Role role = chatEntry.getRole();
        String content = chatEntry.getContent();

        return switch (role) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
        };
    }
}
