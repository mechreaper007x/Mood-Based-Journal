package com.example.moodjournal.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;




@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryAnalysisResult {

    
    private String primaryEmotion;

    
    private List<String> nuanceTags;

    
    private VADScores vadScores;

    
    private Integer riskScore;

    
    private Map<String, Integer> emotionBreakdown;
    private List<String> cognitiveDistortions;
    private String emotionalTrajectory;
    private List<String> personalizedSuggestions;

    private String narrativeInsight; 

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
