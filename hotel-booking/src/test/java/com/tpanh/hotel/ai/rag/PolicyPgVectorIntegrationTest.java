package com.tpanh.hotel.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        properties = {
            "spring.docker.compose.enabled=false",
            "booking.expiration.cron=-",
            "ai.policy-rag.rag-enabled=true"
        })
@Import(PolicyPgVectorIntegrationTest.TestEmbeddingConfiguration.class)
class PolicyPgVectorIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("hotel_booking_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PolicyRetriever retriever;

    @Autowired
    private PolicyKnowledgeIndex index;

    @Test
    void flywayCreatesPgVectorSchemaWithProductionDimension() {
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')", Boolean.class))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                        """
                        SELECT format_type(attribute.atttypid, attribute.atttypmod)
                        FROM pg_attribute attribute
                        WHERE attribute.attrelid = 'policy_vector_store'::regclass
                          AND attribute.attname = 'embedding'
                        """,
                        String.class))
                .isEqualTo("vector(1536)");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM policy_vector_store", Integer.class))
                .isPositive();
        assertThat(jdbcTemplate.queryForObject(
                        """
                        SELECT count(*) FROM policy_vector_store
                        WHERE metadata->>'hotel' = 'HANBAY HOTEL'
                          AND metadata->>'language' = 'vi'
                          AND metadata->>'title' IS NOT NULL
                          AND metadata->>'category' IS NOT NULL
                          AND metadata->>'section' IS NOT NULL
                        """,
                        Integer.class))
                .isEqualTo(jdbcTemplate.queryForObject("SELECT count(*) FROM policy_vector_store", Integer.class));
    }

    @Test
    void vectorSearchReturnsPolicySourcesAndRejectsUnsupportedTopicsWithTestEmbeddings() {
        assertDocument("Giờ nhận phòng là mấy giờ?", "HANBAY-CHECK-IN-POLICY");
        assertDocument("Khách có thể hủy booking không?", "HANBAY-CANCELLATION-POLICY");
        assertDocument("Trẻ em có tính vào sức chứa không?", "HANBAY-BOOKING-POLICY");
        assertThat(retriever.retrieve("Khách sạn có sân golf không?").hasEvidence())
                .isFalse();
        assertThat(retriever.retrieve("Khách sạn có cho thú cưng không?").hasEvidence())
                .isFalse();
    }

    @Test
    void repeatedIngestionIsIdempotentAcrossExistingDatabaseState() {
        Integer before = jdbcTemplate.queryForObject("SELECT count(*) FROM policy_vector_store", Integer.class);
        assertThat(index.ingestIfChanged()).isZero();
        assertThat(index.ingestIfChanged()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM policy_vector_store", Integer.class))
                .isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM policy_knowledge_ingestion WHERE corpus_id = 'HANBAY-DEMO-POLICY'",
                        Integer.class))
                .isEqualTo(1);
    }

    private void assertDocument(String question, String documentId) {
        var evidence = retriever.retrieve(question);
        assertThat(evidence.hasEvidence()).isTrue();
        assertThat(evidence.sources()).isNotEmpty().allMatch(source -> source.score() >= 0.18);
        assertThat(evidence.sources())
                .extracting(PolicyRetriever.PolicySource::documentId)
                .contains(documentId);
    }

    @TestConfiguration
    static class TestEmbeddingConfiguration {
        @Bean
        EmbeddingModel embeddingModel() {
            return new DeterministicTestEmbeddingModel();
        }
    }

    /** TEST EMBEDDING only: validates pgvector mechanics without claiming real semantic-provider verification. */
    static final class DeterministicTestEmbeddingModel implements EmbeddingModel {
        private static final int DIMENSIONS = 1536;
        private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            for (int index = 0; index < request.getInstructions().size(); index++) {
                embeddings.add(new Embedding(vector(request.getInstructions().get(index)), index));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            // Use controlled document vectors: policy prose can mention multiple topics.
            // Keyword-counting that prose makes this mechanics test depend on incidental wording.
            int topic =
                    switch (document.getMetadata().get("category").toString()) {
                        case "CHECK_IN" -> 0;
                        case "CANCELLATION" -> 1;
                        case "BOOKING" -> 2;
                        default -> 11;
                    };
            float[] vector = new float[DIMENSIONS];
            vector[20] = 0.1f;
            vector[topic] = 1;
            return vector;
        }

        @Override
        public List<float[]> embed(
                List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
            return documents.stream().map(this::embed).toList();
        }

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }

        private float[] vector(String text) {
            String value = DIACRITICS
                    .matcher(Normalizer.normalize(text, Normalizer.Form.NFD))
                    .replaceAll("")
                    .toLowerCase(Locale.ROOT);
            float[] vector = new float[DIMENSIONS];
            vector[20] = 0.1f;
            if (contains(value, "check-in", "check in", "nhan phong")) vector[0] = 1;
            if (contains(value, "huy", "cancel", "refund", "hoan tien")) vector[1] = 1;
            if (contains(value, "tre em", "suc chua", "capacitychildren", "capacity")) vector[2] = 1;
            if (contains(value, "san golf", "thu cung", "pet")) vector[10] = 1;
            return vector;
        }

        private boolean contains(String value, String... terms) {
            return List.of(terms).stream().anyMatch(value::contains);
        }
    }
}
