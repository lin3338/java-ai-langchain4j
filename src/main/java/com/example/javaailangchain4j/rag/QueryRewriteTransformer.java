package com.example.javaailangchain4j.rag;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rewrites user questions into concise retrieval queries before RAG search.
 */
public class QueryRewriteTransformer implements QueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriteTransformer.class);

    private final ChatLanguageModel chatLanguageModel;

    public QueryRewriteTransformer(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @Override
    public Collection<Query> transform(Query query) {
        String originalQuery = query.text();
        Set<String> queryTexts = new LinkedHashSet<>();
        queryTexts.add(originalQuery);

        try {
            String rewrittenQuery = chatLanguageModel.chat("""
                    你是一个医疗知识库检索查询改写器。
                    请把用户问题改写成更适合知识库检索的中文短查询。
                    要求：
                    1. 保留科室、疾病、医生、时间、地点等关键实体；
                    2. 删除寒暄和无关口语；
                    3. 不回答问题，只输出 1 条改写后的查询；
                    4. 输出不要带编号、解释或标点包装。
                    用户问题：%s
                    """.formatted(originalQuery));

            List<String> candidates = parseCandidates(rewrittenQuery);
            queryTexts.addAll(candidates);
            log.info("RAG query rewrite: original='{}', rewritten={}", originalQuery, candidates);
        } catch (Exception e) {
            log.warn("Query rewrite failed, fallback to original query: {}", originalQuery, e);
        }

        return queryTexts.stream()
                .filter(text -> text != null && !text.isBlank())
                .map(text -> Query.from(text.trim(), query.metadata()))
                .toList();
    }

    private List<String> parseCandidates(String rewrittenQuery) {
        if (rewrittenQuery == null || rewrittenQuery.isBlank()) {
            return List.of();
        }

        List<String> candidates = new ArrayList<>();
        for (String line : rewrittenQuery.split("\\R")) {
            String candidate = line
                    .replaceFirst("^\\s*[-*\\d.、）)]+\\s*", "")
                    .trim();
            if (!candidate.isBlank()) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }
}
