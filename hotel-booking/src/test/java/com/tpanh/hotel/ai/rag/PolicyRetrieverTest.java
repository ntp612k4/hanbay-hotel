package com.tpanh.hotel.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class PolicyRetrieverTest {

    @Test
    void shouldUseSemanticVectorSearchAndBuildCitationFromRetrievedMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        Document document = Document.builder()
                .id("HANBAY-CHECK-IN-POLICY#0")
                .text("Check-in Policy\nThời điểm nhận phòng\nNhận phòng từ 14:00.")
                .metadata(Map.of(
                        "documentId", "HANBAY-CHECK-IN-POLICY",
                        "title", "Check-in Policy",
                        "category", "CHECK_IN",
                        "source", "hotel-booking/src/main/resources/knowledge-base/check-in-policy.md",
                        "section", "Thời điểm nhận phòng",
                        "chunkIndex", 0))
                .score(0.91)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
        PolicyRetriever retriever = new PolicyRetriever(vectorStore, 3, 0.72);

        var evidence = retriever.retrieve("Khách được check-in lúc mấy giờ?");

        assertThat(evidence.context()).contains("14:00");
        assertThat(evidence.sources()).singleElement().satisfies(source -> {
            assertThat(source.documentId()).isEqualTo("HANBAY-CHECK-IN-POLICY");
            assertThat(source.section()).isEqualTo("Thời điểm nhận phòng");
            assertThat(source.chunkIndex()).isZero();
        });
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertThat(request.getValue().getQuery()).isEqualTo("Khách được check-in lúc mấy giờ?");
        assertThat(request.getValue().getTopK()).isEqualTo(3);
        assertThat(request.getValue().getSimilarityThreshold()).isEqualTo(0.72);
    }

    @Test
    void shouldReturnNoEvidenceWhenVectorStoreHasNoMatchAboveThreshold() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        var evidence = new PolicyRetriever(vectorStore, 3, 0.72).retrieve("Khách sạn có sân golf không?");

        assertThat(evidence.hasEvidence()).isFalse();
        assertThat(evidence.sources()).isEmpty();
    }
}
