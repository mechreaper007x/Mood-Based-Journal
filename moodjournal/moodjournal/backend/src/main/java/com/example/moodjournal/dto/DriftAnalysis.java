package com.example.moodjournal.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OCEAN (Big 5) drift analysis result.
 * Detects anomalous personality score changes between assessments.
 * 
 * Personality traits are relatively stable in adults. Drastic changes
 * between assessments may indicate:
 * - Invalid/careless responding
 * - Significant life events (trauma, medication change)
 * - Gaming/manipulation of the assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftAnalysis {

    /** List of detected drift warnings */
    private List<DriftWarning> warnings;

    /** Whether any drift was detected */
    private boolean driftDetected;

    /** Overall assessment validity based on drift */
    private String validityStatus; // VALID, WARNING, BLOCKED

    /**
     * Single trait drift warning.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriftWarning {
        /** Trait name (e.g., "extraversion", "openness") */
        private String trait;

        /** Historical average score */
        private double historicalAverage;

        /** Current score */
        private double currentScore;

        /** Absolute drift magnitude */
        private double driftMagnitude;

        /** Severity: WARNING or BLOCKED */
        private String severity;
    }

    /**
     * Factory for no-history case.
     */
    public static DriftAnalysis noHistory() {
        return DriftAnalysis.builder()
                .driftDetected(false)
                .validityStatus("VALID")
                .warnings(List.of())
                .build();
    }
}
