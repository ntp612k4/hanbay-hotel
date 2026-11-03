CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE policy_vector_store (
    id text PRIMARY KEY,
    content text NOT NULL,
    metadata json NOT NULL,
    embedding vector(1536) NOT NULL
);

CREATE INDEX policy_vector_store_embedding_hnsw_idx
    ON policy_vector_store USING hnsw (embedding vector_cosine_ops);

CREATE TABLE policy_knowledge_ingestion (
    corpus_id varchar(100) PRIMARY KEY,
    checksum char(64) NOT NULL,
    chunk_count integer NOT NULL CHECK (chunk_count > 0),
    ingested_at timestamptz NOT NULL DEFAULT now()
);
