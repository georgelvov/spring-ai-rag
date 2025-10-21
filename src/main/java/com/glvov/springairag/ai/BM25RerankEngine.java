package com.glvov.springairag.ai;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import lombok.Builder;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@code BM25RerankEngine} class implements document ranking using the BM25 algorithm.
 *
 * <p>It is used to rerank a list of documents retrieved from a vector search,
 * highlighting the most relevant documents with respect to a user query.</p>
 *
 * <p>Details:</p>
 * <ul>
 *     <li>Supports multiple languages (English, Russian, German) for text tokenization.</li>
 *     <li>Uses standard BM25 parameters:
 *         <ul>
 *             <li>{@code K} — term frequency saturation coefficient. Higher K increases the weight of repeated terms in a document.</li>
 *             <li>{@code B} — document length normalization coefficient. B=1 fully accounts for document length, B=0 ignores length.</li>
 *         </ul>
 *     </li>
 *     <li>Calculates term frequencies in documents and term rarity in the corpus (IDF).</li>
 *     <li>Sums the BM25 scores for all query terms to get the overall document ranking.</li>
 * </ul>
 */

@Builder
public class BM25RerankEngine {

    @Builder.Default
    private static final LanguageDetector languageDetector = LanguageDetectorBuilder
            .fromLanguages(Language.ENGLISH, Language.GERMAN, Language.RUSSIAN)
            .build();

    // BM25 parameters
    @Builder.Default
    private final double K = 1.2;

    @Builder.Default
    private final double B = 0.75;


    public List<Document> rerank(List<Document> corpus, String query, int limit) {
        if (corpus == null || corpus.isEmpty()) {
            return new ArrayList<>();
        }

        // Compute corpus statistics
        CorpusStats stats = computeCorpusStats(corpus);

        // Tokenize query
        List<String> queryTerms = tokenize(query);

        // Score and sort documents
        return corpus.stream()
                .sorted((d1, d2) -> Double.compare(
                        score(queryTerms, d2, stats),
                        score(queryTerms, d1, stats)
                ))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private CorpusStats computeCorpusStats(List<Document> corpus) {
        Map<String, Integer> docFreq = new HashMap<>();
        Map<Document, List<String>> tokenizedDocs = new HashMap<>();
        int totalLength = 0;
        int totalDocs = corpus.size();

        // Process each document
        for (Document doc : corpus) {
            List<String> tokens = tokenize(doc.getText());
            tokenizedDocs.put(doc, tokens);
            totalLength += tokens.size();

            // Update document frequencies
            Set<String> uniqueTerms = new HashSet<>(tokens);
            for (String term : uniqueTerms) {
                docFreq.put(term, docFreq.getOrDefault(term, 0) + 1);
            }
        }

        double avgDocLength = (double) totalLength / totalDocs;

        return new CorpusStats(docFreq, tokenizedDocs, avgDocLength, totalDocs);
    }

    private double score(List<String> queryTerms, Document doc, CorpusStats stats) {
        List<String> tokens = stats.tokenizedDocs.get(doc);
        if (tokens == null) {
            return 0.0;
        }

        // Calculate term frequencies for this document
        Map<String, Integer> tfMap = new HashMap<>();
        for (String token : tokens) {
            tfMap.put(token, tfMap.getOrDefault(token, 0) + 1);
        }

        int docLength = tokens.size();
        double score = 0.0;

        // Calculate BM25 score
        for (String term : queryTerms) {
            int tf = tfMap.getOrDefault(term, 0); // просто его count - то есть это влияет на его вес в документе
            int df = stats.docFreq.getOrDefault(term, 1);

            // BM25 IDF calculation редкость слова - оно поднимает
            double idf = Math.log(1 + (stats.totalDocs - df + 0.5) / (df + 0.5));

            // BM25 term score calculation
            double numerator = tf * (K + 1);
            double denominator = tf + K * (1 - B + B * docLength / stats.avgDocLength);
            score += idf * (numerator / denominator);
        }

        return score;
    }

    @SneakyThrows
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        try (Analyzer analyzer = detectLanguageAnalyzer(text); TokenStream stream = analyzer.tokenStream(null, text)) {
            stream.reset();

            while (stream.incrementToken()) {
                tokens.add(stream.getAttribute(CharTermAttribute.class).toString());
            }

            stream.end();
        }

        return tokens;
    }

    private Analyzer detectLanguageAnalyzer(String text) {
        Language lang = languageDetector.detectLanguageOf(text);

        return switch (lang) {
            case RUSSIAN -> new RussianAnalyzer();
            case GERMAN -> new GermanAnalyzer();
            default -> new EnglishAnalyzer();
        };
    }

    private record CorpusStats(Map<String, Integer> docFreq,
                               Map<Document, List<String>> tokenizedDocs,
                               double avgDocLength,
                               int totalDocs) {
    }
}