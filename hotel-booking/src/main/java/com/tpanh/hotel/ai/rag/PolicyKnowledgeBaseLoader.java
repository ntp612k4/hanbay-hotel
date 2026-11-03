package com.tpanh.hotel.ai.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public final class PolicyKnowledgeBaseLoader {

    private static final String RESOURCE_PATTERN = "classpath*:knowledge-base/*.md";
    private static final String LOCAL_DEVELOPMENT_RESOURCE_PATTERN =
            "file:src/main/resources/knowledge-base/*-policy.md";
    private static final List<String> REQUIRED_METADATA =
            List.of("documentId", "title", "category", "source", "version", "hotel", "language");

    public List<Document> load() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(RESOURCE_PATTERN);
            if (resources.length == 0) {
                resources = resolver.getResources(LOCAL_DEVELOPMENT_RESOURCE_PATTERN);
            }
            List<Document> documents = new ArrayList<>();
            for (Resource resource : resources) {
                documents.addAll(loadResource(resource));
            }
            if (documents.isEmpty()) {
                throw new IllegalStateException(
                        "Không tìm thấy policy document trong classpath hoặc local development path");
            }
            return List.copyOf(documents);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tải policy knowledge base", exception);
        }
    }

    private List<Document> loadResource(Resource resource) throws IOException {
        String markdown = resource.getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        ParsedMarkdown parsed = parseFrontMatter(markdown, resource.getFilename());
        parsed.metadata().put("checksum", checksum(markdown));
        parsed.metadata().put("corpus", "HANBAY-DEMO-POLICY");
        return chunk(parsed);
    }

    private String checksum(String markdown) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(markdown.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }

    private ParsedMarkdown parseFrontMatter(String markdown, String filename) {
        if (!markdown.startsWith("---\n")) {
            throw new IllegalStateException("Thiếu metadata trong " + filename);
        }
        int end = markdown.indexOf("\n---\n", 4);
        if (end < 0) {
            throw new IllegalStateException("Metadata không hợp lệ trong " + filename);
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        for (String line : markdown.substring(4, end).split("\n")) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                metadata.put(
                        line.substring(0, separator).trim(),
                        line.substring(separator + 1).trim());
            }
        }
        for (String key : REQUIRED_METADATA) {
            if (!metadata.containsKey(key) || metadata.get(key).toString().isBlank()) {
                throw new IllegalStateException("Thiếu metadata " + key + " trong " + filename);
            }
        }
        return new ParsedMarkdown(metadata, markdown.substring(end + 5).trim());
    }

    private List<Document> chunk(ParsedMarkdown parsed) {
        String[] lines = parsed.body().split("\n");
        String documentTitle = "";
        List<Chunk> chunks = new ArrayList<>();
        String heading = null;
        StringBuilder content = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("# ") && documentTitle.isBlank()) {
                documentTitle = line.substring(2).trim();
            }
            if (line.startsWith("### ")) {
                addChunk(chunks, heading, content);
                heading = line.substring(4).trim();
                content = new StringBuilder();
                continue;
            }
            if (heading != null && line.startsWith("## ")) {
                addChunk(chunks, heading, content);
                heading = null;
                content = new StringBuilder();
                continue;
            }
            if (heading != null) {
                content.append(line).append('\n');
            }
        }
        addChunk(chunks, heading, content);

        List<Document> documents = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            Chunk chunk = chunks.get(index);
            Map<String, Object> metadata = new LinkedHashMap<>(parsed.metadata());
            metadata.put("section", chunk.heading());
            metadata.put("chunkIndex", index);
            String id = parsed.metadata().get("documentId") + "#" + index;
            documents.add(Document.builder()
                    .id(id)
                    .text(documentTitle + "\n" + chunk.heading() + "\n" + chunk.content())
                    .metadata(metadata)
                    .build());
        }
        return documents;
    }

    private void addChunk(List<Chunk> chunks, String heading, StringBuilder content) {
        if (heading != null && !content.toString().isBlank()) {
            chunks.add(new Chunk(heading, content.toString().trim()));
        }
    }

    private record ParsedMarkdown(Map<String, Object> metadata, String body) {}

    private record Chunk(String heading, String content) {}
}
