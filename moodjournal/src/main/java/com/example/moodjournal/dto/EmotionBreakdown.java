package com.example.moodjournal.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Response from Gemini emotion analysis - contains emotion percentages
 */
@Data
public class EmotionBreakdown {

    @JsonProperty("emotions")
    private Map<String, Integer> emotions; // e.g. {"anger": 50, "happy": 10, "sadness": 25}

    @JsonProperty("dominant_emotion")
    private String dominantEmotion; // The emotion with highest percentage

    @JsonProperty("dominant_percentage")
    private Integer dominantPercentage; // Percentage of dominant emotion

    @JsonProperty("summary")
    private String summary; // Brief neutral analysis
}
