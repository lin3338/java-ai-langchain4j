package com.example.javaailangchain4j.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class KnowledgeBaseIngestor {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIngestor.class);

    private KnowledgeBaseIngestor() {
    }

    public static void ingestWithStableIds(
            List<TextSegment> segments,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        if (segments == null || segments.isEmpty()) {
            log.warn("Knowledge base ingestion skipped: no text segments found");
            return;
        }

        List<String> ids = segments.stream()
                .map(KnowledgeBaseIngestor::stableId)
                .toList();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(ids, embeddings, segments);

        log.info("Knowledge base ingestion finished: segments={}, stableIds={}", segments.size(), true);
    }

    private static String stableId(TextSegment segment) {
        String source = segment.text() + "|" + segment.metadata().toString();
        return "kb-" + sha256(source).substring(0, 32);
    }

    private static String sha256(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
