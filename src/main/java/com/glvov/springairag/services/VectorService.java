package com.glvov.springairag.services;

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
public class VectorService {

    private final VectorStore vectorStore;

    @Value("${chunk.size:200}")
    private int chunkSize;


    public List<Document> save(Resource resource) {
        List<Document> documents = new TextReader(resource).get();
        TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(chunkSize).build();
        List<Document> chunks = textSplitter.apply(documents);

        vectorStore.accept(chunks);

        return chunks;
    }
}
