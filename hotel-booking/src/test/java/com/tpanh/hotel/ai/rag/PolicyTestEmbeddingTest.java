package com.tpanh.hotel.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingOptions;

class PolicyTestEmbeddingTest {
    @Test
    void batchEmbeddingUsesDocumentCategoriesDespiteOverlappingPolicyWording() {
        var model = new PolicyPgVectorIntegrationTest.DeterministicTestEmbeddingModel();
        List<Document> documents = new PolicyKnowledgeBaseLoader().load();
        List<float[]> vectors = model.embed(documents, mock(EmbeddingOptions.class), mock(BatchingStrategy.class));

        assertThat(vectors).hasSize(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            float[] vector = vectors.get(i);
            assertThat(vector).containsExactly(model.embed(document));
            assertThat(vector[0])
                    .isEqualTo("CHECK_IN".equals(document.getMetadata().get("category")) ? 1f : 0f);
            assertThat(vector[1])
                    .isEqualTo("CANCELLATION".equals(document.getMetadata().get("category")) ? 1f : 0f);
            assertThat(vector[2])
                    .isEqualTo("BOOKING".equals(document.getMetadata().get("category")) ? 1f : 0f);
            assertThat(vector[10]).isZero();
        }
        assertThat(model.embed("Giờ nhận phòng là mấy giờ?")[0]).isEqualTo(1f);
        assertThat(model.embed("Khách sạn có sân golf không?")[10]).isEqualTo(1f);
    }
}
