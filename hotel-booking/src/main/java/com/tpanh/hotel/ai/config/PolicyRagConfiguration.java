package com.tpanh.hotel.ai.config;

import com.tpanh.hotel.ai.rag.PolicyKnowledgeBaseLoader;
import com.tpanh.hotel.ai.rag.PolicyKnowledgeIndex;
import com.tpanh.hotel.ai.rag.PolicyRetriever;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIdType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PolicyRagProperties.class)
public class PolicyRagConfiguration {

    @Bean
    PolicyKnowledgeBaseLoader policyKnowledgeBaseLoader() {
        return new PolicyKnowledgeBaseLoader();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "ai.policy-rag.rag-enabled", havingValue = "true")
    static class PgVectorRagConfiguration {

        @Bean("policyVectorStore")
        VectorStore policyVectorStore(
                JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel, PolicyRagProperties properties) {
            return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                    .schemaName("public")
                    .vectorTableName("policy_vector_store")
                    .idType(PgIdType.TEXT)
                    .dimensions(properties.embeddingDimensions())
                    .distanceType(PgDistanceType.COSINE_DISTANCE)
                    .indexType(PgIndexType.HNSW)
                    .initializeSchema(false)
                    .vectorTableValidationsEnabled(true)
                    .build();
        }

        @Bean
        PolicyKnowledgeIndex policyKnowledgeIndex(
                @Qualifier("policyVectorStore") VectorStore vectorStore,
                JdbcTemplate jdbcTemplate,
                PolicyKnowledgeBaseLoader loader) {
            PolicyKnowledgeIndex index = new PolicyKnowledgeIndex(vectorStore, jdbcTemplate, loader);
            index.ingestIfChanged();
            return index;
        }

        @Bean
        PolicyRetriever policyRetriever(
                @Qualifier("policyVectorStore") VectorStore vectorStore, PolicyRagProperties properties) {
            return new PolicyRetriever(vectorStore, properties.topK(), properties.similarityThreshold());
        }
    }
}
