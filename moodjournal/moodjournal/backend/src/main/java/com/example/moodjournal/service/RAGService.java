package com.example.moodjournal.service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.moodjournal.model.RAGDocument;
import jakarta.annotation.PostConstruct;

/**
 * Service for Simple RAG (Retrieval-Augmented Generation).
 * Uses TF-IDF style keyword matching to find semantically relevant documents.
 * No external API calls are needed for retrieval - fast and free.
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);
    private static final String DATASET_NAME = "mistral_master_raw_dataset.csv";

    private final List<RAGDocument> documents = new ArrayList<>();

    // IDF (Inverse Document Frequency) cache for all terms
    private final Map<String, Double> idfCache = new HashMap<>();

    @PostConstruct
    public void init() {
        loadFromCSV();
        if (!documents.isEmpty()) {
            buildIdfCache();
            log.info("Simple RAG System Ready. Docs: {}, Unique Terms: {}",
                    documents.size(), idfCache.size());
        } else {
            log.warn("RAG System: No documents found in CSV.");
        }
    }

    /**
     * Tokenize text: lowercase, split on non-word chars, filter short words.
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank())
            return Collections.emptySet();
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 2) // Skip very short words
                .collect(Collectors.toSet());
    }

    /**
     * Build IDF cache from all documents.
     * IDF = log(N / df) where N is total docs and df is document frequency.
     */
    private void buildIdfCache() {
        int N = documents.size();
        Map<String, Integer> termDocCounts = new HashMap<>();

        for (RAGDocument doc : documents) {
            Set<String> tokens = tokenize(doc.getText() + " " + doc.getCategory() + " " + doc.getSubtype());
            doc.setTokens(tokens); // Cache tokens on document for fast lookup
            for (String token : tokens) {
                termDocCounts.merge(token, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : termDocCounts.entrySet()) {
            // Add 1 to prevent division by zero
            double idf = Math.log((double) N / (entry.getValue() + 1));
            idfCache.put(entry.getKey(), idf);
        }
    }

    /**
     * Calculate TF-IDF score for a query against a document.
     */
    private double calculateTfIdfScore(Set<String> queryTokens, RAGDocument doc) {
        if (doc.getTokens() == null || doc.getTokens().isEmpty())
            return 0.0;

        double score = 0.0;
        for (String queryTerm : queryTokens) {
            if (doc.getTokens().contains(queryTerm)) {
                // TF = 1 (binary for simplicity), IDF from cache
                double idf = idfCache.getOrDefault(queryTerm, 0.0);
                score += idf;
            }
        }
        return score;
    }

    /**
     * Find documents relevant to the query using TF-IDF keyword matching.
     * This is the "Simple RAG" - no embeddings, no external API calls.
     */
    public List<RAGDocument> findSimilarDocuments(String query, int limit) {
        if (query == null || query.isBlank()) {
            log.debug("[RAG] Empty query, skipping search.");
            return Collections.emptyList();
        }

        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            log.debug("[RAG] No valid tokens in query.");
            return Collections.emptyList();
        }

        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║               🔍 RAG KNOWLEDGE BASE SEARCH                      ║");
        log.info("╠══════════════════════════════════════════════════════════════════╣");
        log.info("║ Query Tokens: {}",
                queryTokens.stream().limit(10).collect(Collectors.joining(", ")));

        List<AbstractMap.SimpleEntry<RAGDocument, Double>> scored = documents.stream()
                .map(doc -> new AbstractMap.SimpleEntry<>(doc, calculateTfIdfScore(queryTokens, doc)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<RAGDocument, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        if (scored.isEmpty()) {
            log.info("║ Result: No matching documents found in knowledge base.");
            log.info("╚══════════════════════════════════════════════════════════════════╝");
            return Collections.emptyList();
        }

        log.info("║ Found {} relevant clinical examples:", scored.size());
        for (int i = 0; i < scored.size(); i++) {
            RAGDocument doc = scored.get(i).getKey();
            double score = scored.get(i).getValue();
            String preview = doc.getText().length() > 40
                    ? doc.getText().substring(0, 40) + "..."
                    : doc.getText();
            log.info("║   {}. [Score: {:.2f}] {} | {}", i + 1, score, doc.getCategory(), preview);
        }
        log.info("╚══════════════════════════════════════════════════════════════════╝");

        return scored.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    // ========================================================================
    // CSV LOADING (Same as before)
    // ========================================================================

    private void loadFromCSV() {
        documents.clear();
        try (InputStream is = getClass().getResourceAsStream("/" + DATASET_NAME)) {
            if (is == null) {
                log.error("CSV file not found in resources: {}", DATASET_NAME);
                return;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String fullText = sb.toString();
                parseCSV(fullText);
                log.info("Loaded {} documents from CSV.", documents.size());
            }
        } catch (IOException e) {
            log.error("Failed to load CSV: {}", e.getMessage());
        }
    }

    private void parseCSV(String fullText) {
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        List<String> row = new ArrayList<>();
        char[] chars = fullText.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\"') {
                if (i + 1 < chars.length && chars[i + 1] == '\"') {
                    field.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                row.add(field.toString());
                field.setLength(0);
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (field.length() > 0 || !row.isEmpty()) {
                    row.add(field.toString());
                    field.setLength(0);
                }
                if (!row.isEmpty()) {
                    addDocumentFromRow(row);
                    row.clear();
                }
            } else {
                field.append(c);
            }
        }
    }

    private void addDocumentFromRow(List<String> row) {
        if (row.size() < 6 || row.get(0).equalsIgnoreCase("id"))
            return;
        try {
            documents.add(new RAGDocument(
                    row.get(0).trim(), row.get(1).trim(), row.get(2).trim(),
                    row.get(3).trim(), row.get(4).trim(), row.get(5).trim()));
        } catch (Exception ignored) {
        }
    }
}
