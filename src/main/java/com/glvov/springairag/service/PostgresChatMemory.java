package com.glvov.springairag.service;

import com.glvov.springairag.mapper.ChatEntryMapper;
import com.glvov.springairag.model.Chat;
import com.glvov.springairag.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostgresChatMemory implements ChatMemory {

    private final ChatEntryMapper chatEntryMapper;
    private final ChatRepository chatMemoryRepository;

    @Value("${spring.ai.chat.memory.max-messages}")
    private final int maxMessages;


    @Override
    public void add(String conversationId, List<Message> messages) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId))
                .orElseThrow();

        messages.stream()
                .map(chatEntryMapper::toChatEntry)
                .forEach(chat::addChatEntry);

        chatMemoryRepository.save(chat);
    }

    @Override
    public List<Message> get(String conversationId) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId))
                .orElseThrow();

        long messagesToSkip = Math.max(0,chat.getHistory().size() - maxMessages);

        return chat.getHistory().stream()
                .skip(messagesToSkip)
                .map(chatEntryMapper::toMessage)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        // not implemented
    }
}
