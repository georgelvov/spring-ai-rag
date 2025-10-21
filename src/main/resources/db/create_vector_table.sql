-- Расширение для работы с векторами
CREATE EXTENSION IF NOT EXISTS vector;

-- Таблица для векторного хранилища
CREATE TABLE IF NOT EXISTS vector_store
(
    id        VARCHAR(255) PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(1024)
);

-- Индекс HNSW для быстрого векторного поиска
CREATE INDEX IF NOT EXISTS vector_store_hnsw_index ON vector_store USING hnsw (embedding vector_cosine_ops);