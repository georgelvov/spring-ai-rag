package com.glvov.springairag.service;

import com.glvov.springairag.model.entity.LoadedDocument;
import com.glvov.springairag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {


    // ResourcePatternResolver - интерфейс Spring для разрешения ресурсов по шаблону
    private final ResourcePatternResolver resolver;

    // VectorStore - интерфейс Spring AI для работы с векторными хранилищами
    // (PGVector, Chroma, Redis и др.) для хранения эмбеддингов документов
    //  Эмбеддинги - числовые векторные представления текста, capturing semantic meaning
    private final VectorStore vectorStore;

    private final DocumentRepository repository;

    // chunkSize - размер чанка (фрагмента) для разделения документов
    @Value("${app.chunkSize:500}")
    private int chunkSize;

    // Путь к документам для обработки (например: "classpath*:documents/*.txt")
    @Value("${app.document-path:classpath*:documents/*.txt}")
    private String documentPath;


    @SneakyThrows
    public void loadDocuments() {
        List<Resource> documents = Arrays.stream(resolver.getResources(documentPath)).toList();

        log.info("Найдено документов для обработки: {}", documents.size());

        // Создаем TextSplitter для разделения текста на чанки (фрагменты)
        // TokenTextSplitter - разделитель, который считает токены (а не символы/слова)
        // Токенизация - процесс разбиения текста на tokens (слова, subwords)
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize) // Устанавливаем размер чанка
                .build();

        for (Resource document : documents) {
            try {
                log.info("Обработка документа: {}", document.getFilename());

                String filename = document.getFilename();
                String contentHash = calculateContentHash(document);

                // Проверяем, не был ли уже обработан этот документ с таким же хешем
                // (чтобы избежать дублирования обработки неизмененных файлов)
                if (repository.existsByFilenameAndContentHash(filename, contentHash)) {
                    log.info("Документ уже обработан: {}", filename);
                    continue;
                }

                // Создаем TextReader для чтения содержимого документа
                TextReader reader = new TextReader(document);

                // Читаем документ и получаем список объектов Document
                // Document - объект Spring AI, содержащий текст и метаданные
                List<Document> originalDocs = reader.read();

                // Обогащаем документы метаданными:
                // - filename: имя файла
                // - source: источник документа
                // - processed_at: дата обработки
                List<Document> enrichedDocs = originalDocs.stream()
                        .map(doc -> new Document(
                                doc.getFormattedContent(),
                                Map.of(
                                        "filename", Objects.requireNonNull(document.getFilename()),
                                        "source", "internal_docs",
                                        "processed_at", LocalDate.now().toString()
                                )
                        ))
                        .toList();

                // Разделяем обогащенные документы на чанки указанного размера
                // Чанкинг - процесс разделения больших документов на smaller фрагменты
                // для более эффективного поиска и обработки в RAG
                List<Document> chunks = textSplitter.split(enrichedDocs);

                // Добавляем чанки в векторное хранилище
                // Векторное хранилище создает эмбеддинги (векторные представления) текста
                // и сохраняет их для последующего семантического поиска
                vectorStore.add(chunks);

                var loadedDocument = new LoadedDocument()
                        .setFilename(filename)
                        .setContentHash(contentHash) // Хеш для отслеживания изменений
                        .setChunkCount(chunkSize); // Количество чанков (возможно should be chunks.size());

                repository.save(loadedDocument);

                log.info("Обработан и сохранен в БД документ: {} (чанков: {})", document.getFilename(), chunks.size());

            } catch (Exception e) {
                log.error("Ошибка обработки документа: {}", document.getFilename(), e);
            }
        }

        log.info("Загрузка документов завершена");
    }

    // Метод для вычисления MD5 хеша содержимого файла
    // MD5 - алгоритм хеширования, создающий уникальную сигнатуру содержимого
    @SneakyThrows
    private String calculateContentHash(Resource resource) {
        // DigestUtils из Spring - утилита для работы с хешами
        // md5DigestAsHex создает MD5 хеш в виде hex-строки
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }
}
