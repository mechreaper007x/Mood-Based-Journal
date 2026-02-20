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
public class AnalyzedProfile {

    
    private Integer extraversion;
    private Integer agreeableness;
    private Integer conscientiousness;
    private Integer emotionalStability;
    private Integer openness;

    
    private String primaryArchetype;
    private String secondaryArchetype;

    
    private Integer cognitiveEmpathy;
    private Integer affectiveEmpathy;
    private Integer compassionateEmpathy;

    
    private Integer phq9Score;
    private String phq9Severity;

    
    private Integer enneagramType;
    private String enneagramWing;

    
    private Integer eqScore;
    private Integer eqCompletionPercent;

    
    private List<String> detectedStressors;

    
    private String insights;

    
    private Map<String, String> traitAnalysis;

    public void setPhq9Score(Integer score) {
        if (score != null && (score < 0 || score > 27)) {
            
            this.phq9Score = Math.max(0, Math.min(27, score));
        } else {
            this.phq9Score = score;
        }
    }
}
