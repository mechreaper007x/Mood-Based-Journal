package com.example.moodjournal.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;







@Component
public class PatternDiscoveryEngine {

    private static final Logger log = LoggerFactory.getLogger(PatternDiscoveryEngine.class);

    private static final int MIN_ATTACK_FREQUENCY = 3; 
    private static final double MAX_LEGIT_FREQUENCY = 0.0; 
    private static final int MAX_NGRAM_SIZE = 4; 

    public List<String> discoverNewPatterns(List<String> attackSamples, List<String> legitSamples) {
        log.info("[AUTO-IMMUNE] Scanning for new threat patterns...");

        
        Map<String, Integer> attackNgrams = extractNgrams(attackSamples);

        
        Set<String> legitNgrams = extractNgrams(legitSamples).keySet();

        List<String> discoveredRules = new ArrayList<>();

        
        for (Map.Entry<String, Integer> entry : attackNgrams.entrySet()) {
            String ngram = entry.getKey();
            int frequency = entry.getValue();

            
            if (frequency < MIN_ATTACK_FREQUENCY)
                continue;

            
            if (legitNgrams.contains(ngram)) {
                log.debug("[AUTO-IMMUNE] Discarded '{}' - found in legitimate entries.", ngram);
                continue;
            }

            
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
            String cleanText = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " "); 
            String[] words = cleanText.split("\\s+");

            for (int n = 2; n <= MAX_NGRAM_SIZE; n++) { 
                                                        
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
