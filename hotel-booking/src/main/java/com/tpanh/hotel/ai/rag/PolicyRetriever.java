package com.tpanh.hotel.ai.rag;

import java.util.List;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

public class PolicyRetriever {

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    public PolicyRetriever(VectorStore vectorStore, int topK, double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public PolicyEvidence retrieve(String question) {
        List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build());
        List<PolicySource> sources = matches.stream()
                .map(document -> new PolicySource(
                        Objects.toString(document.getMetadata().get("documentId")),
                        Objects.toString(document.getMetadata().get("title")),
                        Objects.toString(document.getMetadata().get("category")),
                        Objects.toString(document.getMetadata().get("source")),
                        Objects.toString(document.getMetadata().get("section")),
                        Integer.parseInt(Objects.toString(document.getMetadata().get("chunkIndex"))),
                        document.getScore()))
                .distinct()
                .toList();
        String context = matches.stream()
                .map(Document::getText)
                .distinct()
                .reduce((left, right) -> left + "\n\n---\n\n" + right)
                .orElse("");
        return new PolicyEvidence(context, sources);
    }

    public record PolicyEvidence(String context, List<PolicySource> sources) {
        public boolean hasEvidence() {
            return !context.isBlank() && !sources.isEmpty();
        }
    }

    public record PolicySource(
            String documentId,
            String title,
            String category,
            String source,
            String section,
            int chunkIndex,
            Double score) {}
}
