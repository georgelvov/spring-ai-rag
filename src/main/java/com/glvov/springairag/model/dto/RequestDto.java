package com.glvov.springairag.model.dto;

import java.util.UUID;

public record RequestDto(UUID chatId, String question) {
}
