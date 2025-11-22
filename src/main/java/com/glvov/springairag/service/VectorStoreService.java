package com.glvov.springairag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorStoreService {

    @Value("${spring.ai.vectorstore.chunk-size:100}")
    private final int chunkSize;

    private final VectorStore vectorStore;


    public List<Document> save(Resource resource) {
        List<Document> documents = new TextReader(resource).get();

        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize) // max tokens for one chunk
                .build();

        List<Document> chunks = textSplitter.apply(documents);

        vectorStore.accept(chunks);

        return chunks;
    }
}
