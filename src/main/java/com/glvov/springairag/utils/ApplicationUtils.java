package com.glvov.springairag.utils;

import java.util.Collection;
import java.util.stream.Stream;

public final class ApplicationUtils {

    private ApplicationUtils() {
    }

    public static <T> Stream<T> streamOfNullable(Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }
}
