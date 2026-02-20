package com.example.moodjournal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;











@Service
public class AIResponseValidator {

    private static final Logger logger = LoggerFactory.getLogger(AIResponseValidator.class);
    private final ObjectMapper objectMapper;

    
    public static final double CONFIDENCE_THRESHOLD = 0.5;

    
    private static final Set<String> VALID_EMOTIONS = Set.of(
            "happiness", "sadness", "anger", "fear", "surprise", "disgust", "contempt");

    
    private static final Set<String> VALID_RISK_LEVELS = Set.of(
            "LOW", "MEDIUM", "HIGH", "CRISIS");

    
    private static final Set<String> VALID_MOOD_CATEGORIES = Set.of(
            "HAPPY", "SAD", "ANXIOUS", "ANGRY", "CALM", "NEUTRAL");

    public AIResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    



    public ValidationResult validateEmotionBreakdown(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isBlank()) {
                return ValidationResult.invalid("Empty response");
            }

            JsonNode root = objectMapper.readTree(jsonResponse);

            
            if (!root.has("emotions") || !root.has("dominantEmotion") || !root.has("overallConfidence")) {
                return ValidationResult.invalid("Missing required fields");
            }

            JsonNode emotions = root.get("emotions");
            String dominantEmotion = root.get("dominantEmotion").asText().toLowerCase();
            double overallConfidence = root.get("overallConfidence").asDouble();

            
            if (!VALID_EMOTIONS.contains(dominantEmotion)) {
                logger.warn("Invalid dominant emotion: {}", dominantEmotion);
                return ValidationResult.invalid("Invalid dominant emotion: " + dominantEmotion);
            }

            
            int totalPercentage = 0;
            int highestPercentage = -1;
            String calculatedDominant = null;
            List<String> missingEmotions = new ArrayList<>();

            for (String emotion : VALID_EMOTIONS) {
                if (!emotions.has(emotion)) {
                    missingEmotions.add(emotion);
                    continue;
                }

                JsonNode emotionNode = emotions.get(emotion);
                if (!emotionNode.has("percentage") || !emotionNode.has("confidence")) {
                    return ValidationResult.invalid("Emotion " + emotion + " missing percentage or confidence");
                }

                int percentage = emotionNode.get("percentage").asInt();
                double confidence = emotionNode.get("confidence").asDouble();

                
                if (percentage < 0 || percentage > 100) {
                    return ValidationResult.invalid("Invalid percentage for " + emotion + ": " + percentage);
                }
                if (confidence < 0.0 || confidence > 1.0) {
                    return ValidationResult.invalid("Invalid confidence for " + emotion + ": " + confidence);
                }

                totalPercentage += percentage;

                if (percentage > highestPercentage) {
                    highestPercentage = percentage;
                    calculatedDominant = emotion;
                }
            }

            if (!missingEmotions.isEmpty()) {
                logger.warn("Missing emotions in response: {}", missingEmotions);
                return ValidationResult.invalid("Missing emotions: " + missingEmotions);
            }

            
            
            if (Math.abs(totalPercentage - 100) > 0) {
                if (Math.abs(totalPercentage - 100) > 5) {
                    logger.warn("Percentages sum to {} instead of 100 (outside tolerance)", totalPercentage);
                    return ValidationResult.invalid("Percentages sum to " + totalPercentage + " (expected 100)");
                } else {
                    logger.info("Percentages sum to {} - normalizing", totalPercentage);
                    
                    
                    
                    
                }
            }

            
            if (calculatedDominant != null && !calculatedDominant.equals(dominantEmotion)) {
                logger.warn("Dominant emotion mismatch: declared={}, calculated={}", dominantEmotion,
                        calculatedDominant);
            }

            
            if (overallConfidence < 0.0 || overallConfidence > 1.0) {
                return ValidationResult.invalid("Invalid overall confidence: " + overallConfidence);
            }

            return ValidationResult.valid(overallConfidence);

        } catch (Exception e) {
            logger.error("Failed to parse emotion breakdown response: {}", e.getMessage());
            return ValidationResult.invalid("JSON parse error: " + e.getMessage());
        }
    }

    



    public ValidationResult validateRiskAssessment(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isBlank()) {
                return ValidationResult.invalid("Empty response");
            }

            JsonNode root = objectMapper.readTree(jsonResponse);

            
            if (!root.has("riskScore") || !root.has("riskLevel") || !root.has("confidence")) {
                return ValidationResult.invalid("Missing required fields");
            }

            int riskScore = root.get("riskScore").asInt();
            String riskLevel = root.get("riskLevel").asText().toUpperCase();
            double confidence = root.get("confidence").asDouble();

            
            if (riskScore < 0 || riskScore > 10) {
                return ValidationResult.invalid("Invalid risk score: " + riskScore);
            }

            
            if (!VALID_RISK_LEVELS.contains(riskLevel)) {
                return ValidationResult.invalid("Invalid risk level: " + riskLevel);
            }

            
            String expectedLevel = mapScoreToRiskLevel(riskScore);
            if (!expectedLevel.equals(riskLevel)) {
                logger.warn("Risk level mismatch: score={} suggests {}, AI returned {}",
                        riskScore, expectedLevel, riskLevel);
                
            }

            
            if (confidence < 0.0 || confidence > 1.0) {
                return ValidationResult.invalid("Invalid confidence: " + confidence);
            }

            return ValidationResult.valid(confidence);

        } catch (Exception e) {
            logger.error("Failed to parse risk assessment response: {}", e.getMessage());
            return ValidationResult.invalid("JSON parse error: " + e.getMessage());
        }
    }

    


    public ValidationResult validateMoodSuggestion(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isBlank()) {
                return ValidationResult.invalid("Empty response");
            }

            JsonNode root = objectMapper.readTree(jsonResponse);

            
            if (!root.has("emotion") || !root.has("category") || !root.has("intensity")) {
                return ValidationResult.invalid("Missing required fields");
            }

            String category = root.get("category").asText().toUpperCase();
            int intensity = root.get("intensity").asInt();
            double confidence = root.has("confidence") ? root.get("confidence").asDouble() : 0.7;

            
            if (!VALID_MOOD_CATEGORIES.contains(category)) {
                return ValidationResult.invalid("Invalid mood category: " + category);
            }

            
            if (intensity < 1 || intensity > 10) {
                return ValidationResult.invalid("Invalid intensity: " + intensity);
            }

            return ValidationResult.valid(confidence);

        } catch (Exception e) {
            logger.error("Failed to parse mood suggestion response: {}", e.getMessage());
            return ValidationResult.invalid("JSON parse error: " + e.getMessage());
        }
    }

    


    public ValidationResult validateDailyQuote(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isBlank()) {
                return ValidationResult.invalid("Empty response");
            }

            JsonNode root = objectMapper.readTree(jsonResponse);

            if (!root.has("quote") || !root.has("author")) {
                return ValidationResult.invalid("Missing quote or author");
            }

            String quote = root.get("quote").asText();
            String author = root.get("author").asText();

            if (quote.isBlank() || author.isBlank()) {
                return ValidationResult.invalid("Empty quote or author");
            }

            
            if (quote.length() > 500) {
                logger.warn("Quote unusually long: {} chars", quote.length());
            }

            boolean verified = root.has("verified") && root.get("verified").asBoolean();
            return ValidationResult.valid(verified ? 0.9 : 0.6);

        } catch (Exception e) {
            logger.error("Failed to parse daily quote response: {}", e.getMessage());
            return ValidationResult.invalid("JSON parse error: " + e.getMessage());
        }
    }

    


    public boolean shouldFallbackToLexicon(double confidence) {
        return confidence < CONFIDENCE_THRESHOLD;
    }

    


    private String mapScoreToRiskLevel(int score) {
        if (score >= 9)
            return "CRISIS";
        if (score >= 7)
            return "HIGH";
        if (score >= 4)
            return "MEDIUM";
        return "LOW";
    }

    


    public static class ValidationResult {
        private final boolean valid;
        private final double confidence;
        private final String failureReason;

        private ValidationResult(boolean valid, double confidence, String failureReason) {
            this.valid = valid;
            this.confidence = confidence;
            this.failureReason = failureReason;
        }

        public static ValidationResult valid(double confidence) {
            return new ValidationResult(true, confidence, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, 0.0, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getFailureReason() {
            return failureReason;
        }

        @Override
        public String toString() {
            return valid
                    ? "ValidationResult{valid=true, confidence=" + confidence + "}"
                    : "ValidationResult{valid=false, reason='" + failureReason + "'}";
        }
    }
}
