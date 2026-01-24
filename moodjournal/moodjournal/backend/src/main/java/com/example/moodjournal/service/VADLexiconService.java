package com.example.moodjournal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * VAD Lexicon Service - Provides local, deterministic VAD scoring
 * using a pre-built lexicon of emotion words.
 * 
 * This acts as the first layer of the Ensemble Risk Engine,
 * providing fast, reliable scoring without AI dependency.
 */
@Service
public class VADLexiconService {

    private static final Logger logger = LoggerFactory.getLogger(VADLexiconService.class);

    private final Map<String, double[]> vadLexicon = new HashMap<>();
    private final Set<String> crisisKeywords = new HashSet<>();
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    // Default crisis keywords for fallback
    private static final Set<String> DEFAULT_CRISIS_KEYWORDS = Set.of(
            "suicide", "suicidal", "kill", "dying", "death", "harm", "hopeless");

    @PostConstruct
    public void init() {
        loadLexicon();
    }

    private void loadLexicon() {
        try {
            ClassPathResource resource = new ClassPathResource("vad_lexicon.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(inputStream);

            // Load regular words
            JsonNode words = root.get("words");
            if (words != null) {
                Iterator<String> fieldNames = words.fieldNames();
                while (fieldNames.hasNext()) {
                    String word = fieldNames.next();
                    JsonNode vadNode = words.get(word);
                    double v = vadNode.get("v").asDouble();
                    double a = vadNode.get("a").asDouble();
                    double d = vadNode.get("d").asDouble();
                    vadLexicon.put(word.toLowerCase(), new double[] { v, a, d });
                }
            }

            // Load crisis keywords
            JsonNode crisisNode = root.get("crisis_keywords");
            if (crisisNode != null) {
                Iterator<String> fieldNames = crisisNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String phrase = fieldNames.next();
                    if (phrase.startsWith("_"))
                        continue; // Skip metadata

                    crisisKeywords.add(phrase.toLowerCase());

                    // Also add to lexicon if it's a single word
                    if (!phrase.contains(" ")) {
                        JsonNode vadNode = crisisNode.get(phrase);
                        double v = vadNode.get("v").asDouble();
                        double a = vadNode.get("a").asDouble();
                        double d = vadNode.get("d").asDouble();
                        vadLexicon.put(phrase.toLowerCase(), new double[] { v, a, d });
                    }
                }
            } else {
                crisisKeywords.addAll(DEFAULT_CRISIS_KEYWORDS);
            }

            logger.info("VAD Lexicon loaded: {} words, {} crisis keywords", vadLexicon.size(), crisisKeywords.size());
        } catch (IOException e) {
            logger.error("Failed to load VAD lexicon: {}", e.getMessage());
            crisisKeywords.addAll(DEFAULT_CRISIS_KEYWORDS);
        }
    }

    /**
     * Analyzes text and returns aggregated VAD scores.
     * 
     * @param text The text to analyze
     * @return Map with "valence", "arousal", "dominance" keys (0.0-1.0)
     */
    public Map<String, Double> analyzeText(String text) {
        if (text == null || text.isBlank()) {
            return getDefaultVAD();
        }

        String lowerText = text.toLowerCase();
        List<double[]> foundScores = new ArrayList<>();

        var matcher = WORD_PATTERN.matcher(lowerText);
        while (matcher.find()) {
            String word = matcher.group();
            if (vadLexicon.containsKey(word)) {
                foundScores.add(vadLexicon.get(word));
            }
        }

        if (foundScores.isEmpty()) {
            return getDefaultVAD();
        }

        // Calculate weighted average (could be enhanced with TF-IDF later)
        double sumV = 0, sumA = 0, sumD = 0;
        for (double[] vad : foundScores) {
            sumV += vad[0];
            sumA += vad[1];
            sumD += vad[2];
        }

        int count = foundScores.size();
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("valence", Math.round((sumV / count) * 100.0) / 100.0);
        result.put("arousal", Math.round((sumA / count) * 100.0) / 100.0);
        result.put("dominance", Math.round((sumD / count) * 100.0) / 100.0);

        return result;
    }

    /**
     * Calculates a risk score (0-10) from VAD values.
     * Low valence + low dominance = high risk
     * Crisis keywords detected = maximum risk boost
     * 
     * @param text The text to analyze
     * @return Risk score from 0 (safe) to 10 (crisis)
     */
    public int calculateRiskScore(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        // Check for crisis keywords first
        String lowerText = text.toLowerCase();
        int crisisKeywordCount = 0;
        for (String keyword : crisisKeywords) {
            if (lowerText.contains(keyword)) {
                crisisKeywordCount++;
            }
        }

        // If multiple crisis keywords, return high risk immediately
        if (crisisKeywordCount >= 2) {
            return 9;
        }

        // Get VAD scores
        Map<String, Double> vad = analyzeText(text);
        double valence = vad.get("valence");
        double dominance = vad.get("dominance");

        // Risk formula: (1 - valence) * 0.6 + (1 - dominance) * 0.4
        // Low valence (sadness) and low dominance (helplessness) = high risk
        double baseRisk = ((1 - valence) * 0.6 + (1 - dominance) * 0.4) * 8;

        // Add crisis keyword boost
        if (crisisKeywordCount == 1) {
            baseRisk = Math.min(10, baseRisk + 3);
        }

        return (int) Math.round(Math.min(10, Math.max(0, baseRisk)));
    }

    /**
     * Returns detected crisis keywords from text.
     */
    public List<String> detectCrisisKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String lowerText = text.toLowerCase();
        List<String> detected = new ArrayList<>();

        for (String keyword : crisisKeywords) {
            if (lowerText.contains(keyword)) {
                detected.add(keyword);
            }
        }

        return detected;
    }

    /**
     * Returns the count of lexicon words found in text.
     */
    public int getMatchedWordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        String lowerText = text.toLowerCase();
        int count = 0;

        var matcher = WORD_PATTERN.matcher(lowerText);
        while (matcher.find()) {
            String word = matcher.group();
            if (vadLexicon.containsKey(word)) {
                count++;
            }
        }

        return count;
    }

    private Map<String, Double> getDefaultVAD() {
        Map<String, Double> defaultVAD = new LinkedHashMap<>();
        defaultVAD.put("valence", 0.50);
        defaultVAD.put("arousal", 0.50);
        defaultVAD.put("dominance", 0.50);
        return defaultVAD;
    }

    /**
     * Check if the lexicon is loaded and ready.
     */
    public boolean isReady() {
        return !vadLexicon.isEmpty();
    }

    /**
     * Get lexicon size for diagnostics.
     */
    public int getLexiconSize() {
        return vadLexicon.size();
    }
}
