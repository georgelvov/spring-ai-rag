package com.glvov.springairag.service;

import com.glvov.springairag.model.LoadedDocument;
import com.glvov.springairag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentLoaderService implements CommandLineRunner {

    private final DocumentRepository documentRepository;
    private final ResourcePatternResolver resolver;
    private final VectorStoreService vectorStoreService;


    @Override
    public void run(String... args) {
        loadDocuments();
    }

    @SneakyThrows
    public void loadDocuments() {
        Resource[] resources = resolver.getResources("classpath:/ai/rag/**/*.txt");

        for (Resource resource : resources) {
            String contentHash = calculateContentHash(resource);

            if (documentExists(resource.getFilename(), contentHash)) {
                continue;
            }

            processDocument(contentHash, resource);
        }
    }

    @SneakyThrows
    private String calculateContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }

    private boolean documentExists(String filename, String contentHash) {
        return documentRepository.existsByFilenameAndContentHash(filename, contentHash);
    }

    private void processDocument(String contentHash, Resource resource) {
        log.info("Start processing RAG document, name: '{}'", resource.getFilename());

        List<Document> chunks = vectorStoreService.save(resource);

        LoadedDocument loadedDocument = LoadedDocument.builder()
                .documentType("txt")
                .chunkCount(chunks.size())
                .filename(resource.getFilename())
                .contentHash(contentHash)
                .build();

        documentRepository.save(loadedDocument);

        log.info("RAG document has been successfully processed");
    }
}
