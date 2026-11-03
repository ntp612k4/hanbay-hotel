package com.tpanh.hotel.ai.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

public final class PolicyKnowledgeIndex {

    private static final String CORPUS_ID = "HANBAY-DEMO-POLICY";

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final PolicyKnowledgeBaseLoader loader;

    public PolicyKnowledgeIndex(VectorStore vectorStore, JdbcTemplate jdbcTemplate, PolicyKnowledgeBaseLoader loader) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.loader = loader;
    }

    public synchronized int ingestIfChanged() {
        List<Document> documents = loader.load();
        String checksum = corpusChecksum(documents);
        List<String> stored = jdbcTemplate.query(
                "SELECT checksum FROM policy_knowledge_ingestion WHERE corpus_id = ?",
                (resultSet, rowNumber) -> resultSet.getString(1),
                CORPUS_ID);
        if (stored.size() == 1 && checksum.equals(stored.get(0))) {
            return 0;
        }

        vectorStore.add(documents);
        deleteStaleChunks(documents.stream().map(Document::getId).toList());
        jdbcTemplate.update(
                """
                INSERT INTO policy_knowledge_ingestion (corpus_id, checksum, chunk_count, ingested_at)
                VALUES (?, ?, ?, now())
                ON CONFLICT (corpus_id) DO UPDATE
                SET checksum = EXCLUDED.checksum,
                    chunk_count = EXCLUDED.chunk_count,
                    ingested_at = EXCLUDED.ingested_at
                """,
                CORPUS_ID,
                checksum,
                documents.size());
        return documents.size();
    }

    private void deleteStaleChunks(List<String> currentIds) {
        String placeholders =
                String.join(",", currentIds.stream().map(ignored -> "?").toList());
        jdbcTemplate.update(
                "DELETE FROM policy_vector_store WHERE id NOT IN (" + placeholders + ")", currentIds.toArray());
    }

    private String corpusChecksum(List<Document> documents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            documents.stream()
                    .sorted((left, right) -> left.getId().compareTo(right.getId()))
                    .forEach(document -> {
                        digest.update(document.getId().getBytes(StandardCharsets.UTF_8));
                        digest.update(document.getText().getBytes(StandardCharsets.UTF_8));
                        digest.update(document.getMetadata().toString().getBytes(StandardCharsets.UTF_8));
                    });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }
}
