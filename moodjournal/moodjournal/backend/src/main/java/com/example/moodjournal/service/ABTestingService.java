package com.example.moodjournal.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;














@Service
public class ABTestingService {

    private static final Logger log = LoggerFactory.getLogger(ABTestingService.class);

    
    private final Map<String, Experiment> experiments = new ConcurrentHashMap<>();

    
    private final Map<String, Map<String, String>> userAssignments = new ConcurrentHashMap<>();

    


    public void registerExperiment(String experimentId, String... variants) {
        if (variants.length < 2) {
            throw new IllegalArgumentException("Experiment must have at least 2 variants");
        }

        experiments.put(experimentId, Experiment.builder()
                .id(experimentId)
                .variants(variants)
                .enabled(true)
                .build());

        log.info("Registered A/B experiment: {} with variants: {}", experimentId, String.join(", ", variants));
    }

    



    public String getVariant(UUID userId, String experimentId) {
        Experiment experiment = experiments.get(experimentId);
        if (experiment == null || !experiment.isEnabled()) {
            return "control"; 
        }

        
        String userIdStr = userId.toString();
        Map<String, String> userExperiments = userAssignments.computeIfAbsent(userIdStr,
                k -> new ConcurrentHashMap<>());

        if (userExperiments.containsKey(experimentId)) {
            return userExperiments.get(experimentId);
        }

        
        int hash = Math.abs((userIdStr + experimentId).hashCode());
        String variant = experiment.getVariants()[hash % experiment.getVariants().length];

        userExperiments.put(experimentId, variant);
        log.debug("Assigned user {} to variant {} for experiment {}", userId, variant, experimentId);

        return variant;
    }

    


    public void trackConversion(UUID userId, String experimentId) {
        String variant = getVariant(userId, experimentId);
        log.info("CONVERSION tracked: experiment={}, variant={}, userId={}", experimentId, variant, userId);
        
    }

    


    public void disableExperiment(String experimentId) {
        Experiment experiment = experiments.get(experimentId);
        if (experiment != null) {
            experiment.setEnabled(false);
            log.info("Disabled experiment: {}", experimentId);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experiment {
        private String id;
        private String[] variants;
        private boolean enabled;
    }
}
