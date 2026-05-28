package com.example.javaailangchain4j.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ReciprocalRankFuser;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Hybrid retriever that combines vector search with a local BM25 retriever.
 */
public class HybridSearchContentRetriever implements ContentRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchContentRetriever.class);
    private static final Pattern LATIN_OR_NUMBER = Pattern.compile("[\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "和", "是", "我", "想", "请", "问", "一个", "可以", "能", "有", "吗", "呢"
    );

    private final ContentRetriever vectorRetriever;
    private final List<Bm25Document> documents;
    private final Map<String, Integer> documentFrequency;
    private final double averageDocumentLength;
    private final int maxResults;

    public HybridSearchContentRetriever(ContentRetriever vectorRetriever, List<TextSegment> segments, int maxResults) {
        this.vectorRetriever = vectorRetriever;
        this.documents = buildDocuments(segments);
        this.documentFrequency = buildDocumentFrequency(documents);
        this.averageDocumentLength = documents.stream()
                .mapToInt(document -> Math.max(document.length, 1))
                .average()
                .orElse(1.0);
        this.maxResults = maxResults;
    }

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> vectorResults = vectorRetriever.retrieve(query);
        List<Content> bm25Results = retrieveByBm25(query.text());

        List<Content> fusedResults = ReciprocalRankFuser.fuse(List.of(vectorResults, bm25Results));
        List<Content> results = deduplicate(fusedResults).stream()
                .limit(maxResults)
                .toList();
        log.info(
                "RAG hybrid search: query='{}', vectorResults={}, bm25Results={}, fusedResults={}, snippets={}",
                query.text(),
                vectorResults.size(),
                bm25Results.size(),
                results.size(),
                snippets(results)
        );
        return results;
    }

    private List<Content> retrieveByBm25(String queryText) {
        List<String> queryTerms = tokenize(queryText);
        if (queryTerms.isEmpty() || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(document -> new ScoredDocument(document, bm25Score(queryTerms, document)))
                .filter(scoredDocument -> scoredDocument.score > 0)
                .sorted((left, right) -> Double.compare(right.score, left.score))
                .limit(maxResults)
                .map(scoredDocument -> Content.from(scoredDocument.document.segment))
                .toList();
    }

    private double bm25Score(List<String> queryTerms, Bm25Document document) {
        double score = 0.0;
        double k1 = 1.5;
        double b = 0.75;

        for (String term : queryTerms) {
            int termFrequency = document.termFrequency.getOrDefault(term, 0);
            if (termFrequency == 0) {
                continue;
            }

            int df = documentFrequency.getOrDefault(term, 0);
            double idf = Math.log(1 + (documents.size() - df + 0.5) / (df + 0.5));
            double denominator = termFrequency + k1 * (1 - b + b * document.length / averageDocumentLength);
            score += idf * (termFrequency * (k1 + 1)) / denominator;
        }

        return score;
    }

    private static List<Bm25Document> buildDocuments(List<TextSegment> segments) {
        return segments.stream()
                .map(segment -> {
                    List<String> terms = tokenize(segment.text());
                    Map<String, Integer> termFrequency = new HashMap<>();
                    for (String term : terms) {
                        termFrequency.merge(term, 1, Integer::sum);
                    }
                    return new Bm25Document(segment, termFrequency, terms.size());
                })
                .toList();
    }

    private static Map<String, Integer> buildDocumentFrequency(List<Bm25Document> documents) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (Bm25Document document : documents) {
            for (String term : document.termFrequency.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        return documentFrequency;
    }

    private static List<Content> deduplicate(Collection<Content> contents) {
        Map<String, Content> deduplicated = new LinkedHashMap<>();
        for (Content content : contents) {
            deduplicated.putIfAbsent(content.textSegment().text(), content);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private static List<String> snippets(List<Content> contents) {
        return contents.stream()
                .map(content -> abbreviate(content.textSegment().text(), 80))
                .toList();
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> terms = new ArrayList<>();
        String normalizedText = text.toLowerCase(Locale.ROOT);

        Matcher matcher = LATIN_OR_NUMBER.matcher(normalizedText);
        while (matcher.find()) {
            String term = matcher.group();
            if (!STOP_WORDS.contains(term)) {
                terms.add(term);
            }
        }

        for (int i = 0; i < normalizedText.length(); i++) {
            char current = normalizedText.charAt(i);
            if (isCjk(current)) {
                String unigram = String.valueOf(current);
                if (!STOP_WORDS.contains(unigram)) {
                    terms.add(unigram);
                }
            }

            if (i + 1 < normalizedText.length()) {
                char next = normalizedText.charAt(i + 1);
                if (isCjk(current) && isCjk(next)) {
                    String bigram = "" + current + next;
                    if (!STOP_WORDS.contains(bigram)) {
                        terms.add(bigram);
                    }
                }
            }
        }

        return terms.stream()
                .filter(term -> !term.isBlank())
                .collect(Collectors.toList());
    }

    private static boolean isCjk(char c) {
        Character.UnicodeScript script = Character.UnicodeScript.of(c);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    private record Bm25Document(TextSegment segment, Map<String, Integer> termFrequency, int length) {
    }

    private record ScoredDocument(Bm25Document document, double score) {
    }
}
