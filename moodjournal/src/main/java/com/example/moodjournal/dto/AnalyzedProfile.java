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

    // Detected Stressors
    private List<String> detectedStressors;

    // Narrative insight from LLM
    private String insights;

    // Optional: Raw trait analysis
    private Map<String, String> traitAnalysis;
}
