package com.example.moodjournal.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of LLM analysis of assessment responses.
 * Contains derived psychological profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedProfile {

    // Big 5 Personality Traits (1-7 scale)
    private Integer extraversion;
    private Integer agreeableness;
    private Integer conscientiousness;
    private Integer emotionalStability;
    private Integer openness;

    // Jungian Archetypes
    private String primaryArchetype;
    private String secondaryArchetype;

    // Empathy Levels (1-10)
    private Integer cognitiveEmpathy;
    private Integer affectiveEmpathy;
    private Integer compassionateEmpathy;

    // PHQ-9 Depression Screening
    private Integer phq9Score;
    private String phq9Severity;

    // Enneagram Type
    private Integer enneagramType;
    private String enneagramWing;

    // EQ-60 Empathy Quotient
    private Integer eqScore;
    private Integer eqCompletionPercent;

    // Detected Stressors
    private List<String> detectedStressors;

    // Narrative insight from LLM
    private String insights;

    // Optional: Raw trait analysis
    private Map<String, String> traitAnalysis;

    public void setPhq9Score(Integer score) {
        if (score != null && (score < 0 || score > 27)) {
            // Clamping instead of throwing to be resilient
            this.phq9Score = Math.max(0, Math.min(27, score));
        } else {
            this.phq9Score = score;
        }
    }
}
