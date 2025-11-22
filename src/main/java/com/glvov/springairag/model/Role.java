package com.glvov.springairag.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum Role {

    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String value;

    public static Role fromString(String value) {
        return Arrays.stream(Role.values())
                .filter(role -> role.value.equals(value))
                .findFirst()
                .orElseThrow();
    }
}
