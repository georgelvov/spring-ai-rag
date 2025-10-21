package com.glvov.springairag.repository;

import com.glvov.springairag.model.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, UUID> {
}
