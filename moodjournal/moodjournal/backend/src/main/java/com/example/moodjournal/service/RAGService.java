package com.example.moodjournal.service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.moodjournal.model.RAGDocument;
import jakarta.annotation.PostConstruct;

/**
 * Service for Vector RAG (Retrieval-Augmented Generation).
 * Uses Gemini Embeddings and Cosine Similarity to find semantic matches.
 * Caches embeddings locally to avoid re-generating them on every restart.
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);
    private static final String DATASET_NAME = "mistral_master_raw_dataset.csv";
    private static final String CACHE_PATH = "rag_cache.dat";

    private final List<RAGDocument> documents = new ArrayList<>();
    private final GeminiService geminiService;

    public RAGService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostConstruct
    public void init() {
        if (loadFromCache()) {
            log.info("RAG System Ready (Loaded from Cache). Docs: {}", documents.size());
        } else {
            log.info("Cache not found or empty. Starting initial ingestion (This may take time)...");
            loadFromCSV();
            if (!documents.isEmpty()) {
                generateEmbeddingsAndCache();
            } else {
                log.warn("RAG System: No documents found in CSV to ingest.");
            }
        }
    }

    private boolean loadFromCache() {
        File cacheFile = new File(CACHE_PATH);
        if (!cacheFile.exists())
            return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
            List<RAGDocument> cachedDocs = (List<RAGDocument>) ois.readObject();
            documents.addAll(cachedDocs);
            return true;
        } catch (Exception e) {
            log.warn("Failed to load RAG cache: {}", e.getMessage());
            return false;
        }
    }

    private void saveToCache() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CACHE_PATH))) {
            oos.writeObject(documents);
            log.info("RAG Cache saved to {}", CACHE_PATH);
        } catch (Exception e) {
            log.error("Failed to save RAG cache: {}", e.getMessage());
        }
    }

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

                // Simple CSV Parser logic (same as before)
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
                log.info("Loaded {} raw documents from CSV.", documents.size());
            }
        } catch (IOException e) {
            log.error("Failed to load CSV: {}", e.getMessage());
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

    private void generateEmbeddingsAndCache() {
        log.info("Generating Embeddings for {} documents...", documents.size());

        int processed = 0;
        int failures = 0;
        boolean keyExpired = false;

        for (RAGDocument doc : documents) {
            try {
                // Combine relevant fields for semantic meaning
                String contentToEmbed = doc.getText() + " " + doc.getCategory() + " " + doc.getSubtype();
                float[] vector = geminiService.getEmbedding(contentToEmbed);
                doc.setEmbedding(vector);

                processed++;
                if (processed % 100 == 0) {
                    log.info("Progress: {}/{} embeddings generated...", processed, documents.size());
                }

                // Tiny sleep to be nice to API
                Thread.sleep(20);

            } catch (Exception e) {
                failures++;
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

                if (errorMsg.contains("api key expired") || errorMsg.contains("400 bad request")) {
                    log.error("CRITICAL: API Key appears to be expired or invalid. Halting RAG ingestion.");
                    keyExpired = true;
                    break;
                }

                log.error("Failed to embed doc {}: {}", doc.getId(), e.getMessage());

                if (failures > 10) {
                    log.error("Too many RAG ingestion failures. Halting to prevent log spam.");
                    break;
                }
            }
        }

        if (processed > 0 && !keyExpired) {
            saveToCache();
            log.info("RAG Ingestion complete. Processed: {}, Failures: {}", processed, failures);
        } else if (keyExpired) {
            log.warn("RAG Ingestion halted due to API Key issue. System will run with limited functionality.");
        }
    }

    /**
     * Vector Search using Cosine Similarity
     */
    public List<RAGDocument> findSimilarDocuments(String query, int limit) {
        try {
            float[] queryVector = geminiService.getEmbedding(query);

            return documents.stream()
                    .filter(doc -> doc.getEmbedding() != null)
                    .map(doc -> new AbstractMap.SimpleEntry<>(doc, cosineSimilarity(queryVector, doc.getEmbedding())))
                    .sorted(Map.Entry.<RAGDocument, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length)
            return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
