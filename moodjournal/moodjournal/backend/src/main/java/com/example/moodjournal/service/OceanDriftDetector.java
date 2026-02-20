package com.example.moodjournal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.moodjournal.dto.DriftAnalysis;
import com.example.moodjournal.dto.DriftAnalysis.DriftWarning;
import com.example.moodjournal.model.AssessmentSession;
import com.example.moodjournal.repository.AssessmentSessionRepository;













@Service
public class OceanDriftDetector {

    private static final Logger log = LoggerFactory.getLogger(OceanDriftDetector.class);

    
    private static final double MAX_ALLOWED_DRIFT = 2.0;

    
    private static final double WARN_DRIFT_THRESHOLD = 1.5;

    private final AssessmentSessionRepository sessionRepository;

    public OceanDriftDetector(AssessmentSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    







    public DriftAnalysis analyzeOceanDrift(UUID userId, Map<String, Integer> currentScores) {
        
        List<AssessmentSession> history = sessionRepository.findTop5ByUserIdOrderByCompletedAtDesc(userId);

        if (history.isEmpty()) {
            log.debug("No assessment history for user {}, skipping drift detection", userId);
            return DriftAnalysis.noHistory();
        }

        Map<String, Double> historicalAvg = calculateHistoricalAverage(history);

        List<DriftWarning> warnings = new ArrayList<>();
        boolean hasBlockingDrift = false;

        for (var entry : currentScores.entrySet()) {
            String trait = entry.getKey();
            double current = entry.getValue();
            double historical = historicalAvg.getOrDefault(trait, current);
            double drift = Math.abs(current - historical);

            if (drift > MAX_ALLOWED_DRIFT) {
                warnings.add(DriftWarning.builder()
                        .trait(trait)
                        .historicalAverage(historical)
                        .currentScore(current)
                        .driftMagnitude(drift)
                        .severity("BLOCKED")
                        .build());
                hasBlockingDrift = true;
                log.warn("OCEAN drift BLOCKED for user {}: {} changed from {} to {} (drift={})",
                        userId, trait, historical, current, drift);
            } else if (drift > WARN_DRIFT_THRESHOLD) {
                warnings.add(DriftWarning.builder()
                        .trait(trait)
                        .historicalAverage(historical)
                        .currentScore(current)
                        .driftMagnitude(drift)
                        .severity("WARNING")
                        .build());
                log.info("OCEAN drift WARNING for user {}: {} changed from {} to {} (drift={})",
                        userId, trait, historical, current, drift);
            }
        }

        String validityStatus = hasBlockingDrift ? "BLOCKED" : warnings.isEmpty() ? "VALID" : "WARNING";

        return DriftAnalysis.builder()
                .warnings(warnings)
                .driftDetected(!warnings.isEmpty())
                .validityStatus(validityStatus)
                .build();
    }

    


    private Map<String, Double> calculateHistoricalAverage(List<AssessmentSession> history) {
        double sumE = 0, sumA = 0, sumC = 0, sumES = 0, sumO = 0;
        int count = 0;

        for (AssessmentSession session : history) {
            if (session.getExtraversion() != null)
                sumE += session.getExtraversion();
            if (session.getAgreeableness() != null)
                sumA += session.getAgreeableness();
            if (session.getConscientiousness() != null)
                sumC += session.getConscientiousness();
            if (session.getEmotionalStability() != null)
                sumES += session.getEmotionalStability();
            if (session.getOpenness() != null)
                sumO += session.getOpenness();
            count++;
        }

        if (count == 0) {
            return Map.of();
        }

        return Map.of(
                "extraversion", sumE / count,
                "agreeableness", sumA / count,
                "conscientiousness", sumC / count,
                "emotionalStability", sumES / count,
                "openness", sumO / count);
    }
}
