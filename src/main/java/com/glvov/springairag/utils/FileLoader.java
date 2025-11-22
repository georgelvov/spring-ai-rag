package com.glvov.springairag.utils;

import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class FileLoader {

    private FileLoader() {
    }

    @SneakyThrows
    public static String loadFile(String fileName) {
        return new String(
                Objects.requireNonNull(FileLoader.class.getResourceAsStream(fileName)).readAllBytes(),
                StandardCharsets.UTF_8
        );
    }
}
