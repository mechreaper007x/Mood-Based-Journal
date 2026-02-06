package com.example.moodjournal.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The "Auto-Immune System".
 * Discovers new attack patterns (N-Grams) from blocked requests
 * and cross-references them with legitimate traffic to generate safe blocking
 * rules.
 */
@Component
public class PatternDiscoveryEngine {

    private static final Logger log = LoggerFactory.getLogger(PatternDiscoveryEngine.class);

    private static final int MIN_ATTACK_FREQUENCY = 3; // Pattern must appear in at least 3 attacks
    private static final double MAX_LEGIT_FREQUENCY = 0.0; // Pattern must NOT appear in legit traffic (Zero Tolerance)
    private static final int MAX_NGRAM_SIZE = 4; // Up to 4-word phrases

    public List<String> discoverNewPatterns(List<String> attackSamples, List<String> legitSamples) {
        log.info("[AUTO-IMMUNE] Scanning for new threat patterns...");

        // 1. Extract N-Grams from specific attack samples
        Map<String, Integer> attackNgrams = extractNgrams(attackSamples);

        // 2. Extract N-Grams from legitimate samples (whitelist)
        Set<String> legitNgrams = extractNgrams(legitSamples).keySet();

        List<String> discoveredRules = new ArrayList<>();

        // 3. Find High-Confidence Attack Patterns
        for (Map.Entry<String, Integer> entry : attackNgrams.entrySet()) {
            String ngram = entry.getKey();
            int frequency = entry.getValue();

            // Rule 1: Must be frequent enough to be a "pattern" (not a one-off)
            if (frequency < MIN_ATTACK_FREQUENCY)
                continue;

            // Rule 2: Must NOT existing in polite society (Legit samples)
            if (legitNgrams.contains(ngram)) {
                log.debug("[AUTO-IMMUNE] Discarded '{}' - found in legitimate entries.", ngram);
                continue;
            }

            // Rule 3: Ignore too short/common stopwords if they slip through
            if (ngram.length() < 4)
                continue;

            log.info("[AUTO-IMMUNE] DISCOVERED THREAT: '{}' (Freq: {})", ngram, frequency);
            discoveredRules.add(ngram);
        }

        return discoveredRules;
    }

    private Map<String, Integer> extractNgrams(List<String> samples) {
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String text : samples) {
            String cleanText = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " "); // Simplify
            String[] words = cleanText.split("\\s+");

            for (int n = 2; n <= MAX_NGRAM_SIZE; n++) { // Only 2-word, 3-word, 4-word phrases (skip single words to
                                                        // avoid banning "the")
                for (int i = 0; i <= words.length - n; i++) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < n; j++) {
                        if (j > 0)
                            sb.append(" ");
                        sb.append(words[i + j]);
                    }
                    String ngram = sb.toString();
                    frequencyMap.put(ngram, frequencyMap.getOrDefault(ngram, 0) + 1);
                }
            }
        }
        return frequencyMap;
    }
}
