package com.glvov.springairag.service;

import com.glvov.springairag.model.entity.AiChat;
import com.glvov.springairag.repository.AiChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final AiChatRepository chatRepository;


    public AiChat createChat(UUID id) {
        AiChat aiChat = new AiChat().setId(id == null ? UUID.randomUUID() : id);
        log.info("Creating new chat: {}", aiChat);
        return chatRepository.save(aiChat);
    }

    public AiChat getChat(UUID id) {
        return chatRepository.findById(id)
                .orElseGet(() -> createChat(id));
    }

    public void saveChat(AiChat chat) {
        chatRepository.save(chat);
    }
}
