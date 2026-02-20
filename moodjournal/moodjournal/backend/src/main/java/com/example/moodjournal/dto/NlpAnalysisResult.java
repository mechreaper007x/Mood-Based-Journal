package com.example.moodjournal.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;




@Data
public class NlpAnalysisResult {

    
    @JsonProperty("token_count")
    private Integer tokenCount;

    private List<String> lemmas;

    private List<Map<String, String>> entities;

    
    @JsonProperty("textblob_polarity")
    private Double textblobPolarity; 

    @JsonProperty("textblob_subjectivity")
    private Double textblobSubjectivity; 

    
    @JsonProperty("vader_positive")
    private Double vaderPositive;

    @JsonProperty("vader_negative")
    private Double vaderNegative;

    @JsonProperty("vader_neutral")
    private Double vaderNeutral;

    @JsonProperty("vader_compound")
    private Double vaderCompound; 

    
    @JsonProperty("base_sentiment")
    private String baseSentiment; 

    private String emotion; 

    private String category; 
}
