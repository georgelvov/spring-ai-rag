package com.glvov.springairag.controller;

import com.glvov.springairag.model.dto.RequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiController {

    private final ChatClient chatClient;


    @PostMapping("/ask")
    public String ask(@RequestBody RequestDto requestDto) {
        return chatClient.prompt(requestDto.question())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, requestDto.chatId()))
                .call()
                .content();
    }
}
