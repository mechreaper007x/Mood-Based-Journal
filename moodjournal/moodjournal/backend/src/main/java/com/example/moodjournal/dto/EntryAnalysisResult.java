package com.example.moodjournal.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of profile-aware psychological analysis of a journal entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryAnalysisResult {

    private String dominantEmotion;

    private Map<String, Integer> emotionBreakdown; // anger, happiness, sadness, anxiety, calmness

    private List<String> cognitiveDistortions; // all-or-nothing, catastrophizing, mind-reading, etc.

    private String emotionalTrajectory; // improving, declining, stable

    private Integer riskScore; // 1-10 mental health concern level

    private List<String> personalizedSuggestions; // based on archetype and profile

    private String narrativeInsight; // 2-3 sentence professional analysis
}
