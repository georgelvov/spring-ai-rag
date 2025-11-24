package com.glvov.springairag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for processing documents into vector embeddings for RAG applications.
 * <p>
 * This service handles the complete document processing pipeline:
 * <ul>
 *   <li>Loading documents from resources</li>
 *   <li>Splitting documents into appropriate chunks</li>
 *   <li>Converting chunks to vector embeddings via embeddings model (like Ollama mxbai-embed-large)</li>
 *   <li>Storing the embeddings in the vector database</li>
 * </ul>
 * <p>
 * The embedding process follows this call chain:
 * <ol>
 *   <li>{@link VectorStore#accept(List)} - Entry point for storing documents (chunks)</li>
 *   <li>{@link AbstractObservationVectorStore#add(List)}</li>
 *   <li>{@link PgVectorStore#doAdd(List)} - PgVector-specific implementation</li>
 *   <li>{@link EmbeddingModel#embed(List, EmbeddingOptions, BatchingStrategy)}</li>
 *   <li>{@link OllamaEmbeddingModel#call(EmbeddingRequest)} - Ollama-specific embedding model</li>
 *   <li>{@link OllamaApi#embed(OllamaApi.EmbeddingsRequest)} - Makes API call to Ollama via /api/embed endpoint</li>
 * </ol>
 */
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
