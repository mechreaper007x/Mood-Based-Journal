package com.example.moodjournal.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;




@Data
public class EmotionBreakdown {

    @JsonProperty("emotions")
    private Map<String, Integer> emotions; 

    @JsonProperty("dominant_emotion")
    private String dominantEmotion; 

    @JsonProperty("dominant_percentage")
    private Integer dominantPercentage; 

    @JsonProperty("summary")
    private String summary; 
}
