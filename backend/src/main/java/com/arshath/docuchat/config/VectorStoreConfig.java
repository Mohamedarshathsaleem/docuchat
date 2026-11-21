package com.arshath.docuchat.config;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory vector store persisted to a JSON file, so the demo runs as a single container
 * with no database. Swapping to pgvector only means replacing this bean
 * (spring-ai-starter-vector-store-pgvector); the rest of the code talks to the VectorStore interface.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Bean
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel, DocuChatProperties properties) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File file = vectorFile(properties);
        if (file.isFile()) {
            store.load(file);
            log.info("Loaded vector store from {}", file);
        }
        return store;
    }

    public static File vectorFile(DocuChatProperties properties) {
        return properties.storageDir().resolve("vector-store.json").toFile();
    }
}
