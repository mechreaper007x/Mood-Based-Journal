package com.example.moodjournal.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Response DTO from the Python NLP microservice
 */
@Data
public class NlpAnalysisResult {

    // spaCy preprocessing
    @JsonProperty("token_count")
    private Integer tokenCount;

    private List<String> lemmas;

    private List<Map<String, String>> entities;

    // TextBlob scores
    @JsonProperty("textblob_polarity")
    private Double textblobPolarity; // -1 to +1

    @JsonProperty("textblob_subjectivity")
    private Double textblobSubjectivity; // 0 to 1

    // VADER scores
    @JsonProperty("vader_positive")
    private Double vaderPositive;

    @JsonProperty("vader_negative")
    private Double vaderNegative;

    @JsonProperty("vader_neutral")
    private Double vaderNeutral;

    @JsonProperty("vader_compound")
    private Double vaderCompound; // -1 to +1 (main score)

    // Ensemble results
    @JsonProperty("base_sentiment")
    private String baseSentiment; // POSITIVE, NEGATIVE, NEUTRAL

    private String emotion; // detected emotion keyword

    private String category; // Mood enum value
}
