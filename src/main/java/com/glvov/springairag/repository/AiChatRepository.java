package com.glvov.springairag.repository;

import com.glvov.springairag.model.entity.AiChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiChatRepository extends JpaRepository<AiChat, UUID> {
}
