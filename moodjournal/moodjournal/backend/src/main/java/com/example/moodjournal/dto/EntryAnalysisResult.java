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

    // ISEAR (Primary Emotion)
    private String primaryEmotion;

    // GoEmotions (Nuance Tags)
    private List<String> nuanceTags;

    // EmoBank (VAD Scores)
    private VADScores vadScores;

    // Derived Risk Score (1-10) - Calculated from Valence/Arousal
    private Integer riskScore;

    // Legacy/Compatible fields (can be derived or removed later)
    private Map<String, Integer> emotionBreakdown;
    private List<String> cognitiveDistortions;
    private String emotionalTrajectory;
    private List<String> personalizedSuggestions;

    private String narrativeInsight; // The "insight" from new prompt

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VADScores {
        private Double valence;
        private Double arousal;
        private Double dominance;
    }
}
