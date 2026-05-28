package com.example.javaailangchain4j.config;

import com.example.javaailangchain4j.rag.HybridSearchContentRetriever;
import com.example.javaailangchain4j.rag.KnowledgeBaseIngestor;
import com.example.javaailangchain4j.rag.QueryRewriteTransformer;
import com.example.javaailangchain4j.store.MongoChatMemoryStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AssistantConfig {

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;
    @Resource
    private EmbeddingModel embeddingModel;
    @Resource
    private ChatLanguageModel qwenChatModel;
    @Resource
    private MongoChatMemoryStore mongoChatMemoryStore;

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }

    @Bean
    ContentRetriever contentRetrieverXiaozhiPinecone() {
        Document document1 = FileSystemDocumentLoader.loadDocument("D:/knowledge/医院信息.md");
        Document document2 = FileSystemDocumentLoader.loadDocument("D:/knowledge/科室信息.md");
        Document document3 = FileSystemDocumentLoader.loadDocument("D:/knowledge/神经内科.md");
        List<Document> documents = Arrays.asList(document1, document2, document3);

        DocumentSplitter documentSplitter = new DocumentByParagraphSplitter(300, 30);
        List<TextSegment> segments = documentSplitter.splitAll(documents);

        KnowledgeBaseIngestor.ingestWithStableIds(segments, embeddingModel, embeddingStore);

        ContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.6)
                .build();

        return new HybridSearchContentRetriever(vectorRetriever, segments, 5);
    }

    @Bean
    RetrievalAugmentor retrievalAugmentorXiaozhi(ContentRetriever contentRetrieverXiaozhiPinecone) {
        return DefaultRetrievalAugmentor.builder()
                .queryTransformer(new QueryRewriteTransformer(qwenChatModel))
                .contentRetriever(contentRetrieverXiaozhiPinecone)
                .build();
    }
}
