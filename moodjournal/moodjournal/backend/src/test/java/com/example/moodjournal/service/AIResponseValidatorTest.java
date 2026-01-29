package com.example.moodjournal.service;

import com.example.moodjournal.service.AIResponseValidator.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIResponseValidator.
 * 
 * Tests the hallucination guardrails:
 * - JSON schema validation
 * - Percentage sum validation
 * - Confidence threshold enforcement
 * - Fallback decision logic
 */
class AIResponseValidatorTest {

    private AIResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AIResponseValidator(new ObjectMapper());
    }

    // ========================================================================
    // EMOTION BREAKDOWN VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Emotion Breakdown Validation")
    class EmotionBreakdownTests {

        @Test
        @DisplayName("Valid response with all 7 emotions should pass")
        void validEmotionBreakdown_shouldPass() {
            String validResponse = """
                    {
                      "emotions": {
                        "happiness": {"percentage": 50, "confidence": 0.9},
                        "sadness": {"percentage": 20, "confidence": 0.8},
                        "anger": {"percentage": 10, "confidence": 0.7},
                        "fear": {"percentage": 5, "confidence": 0.6},
                        "surprise": {"percentage": 5, "confidence": 0.5},
                        "disgust": {"percentage": 5, "confidence": 0.5},
                        "contempt": {"percentage": 5, "confidence": 0.5}
                      },
                      "dominantEmotion": "happiness",
                      "overallConfidence": 0.85,
                      "reasoning": "Test response"
                    }
                    """;

            ValidationResult result = validator.validateEmotionBreakdown(validResponse);

            assertTrue(result.isValid());
            assertEquals(0.85, result.getConfidence(), 0.01);
            assertNull(result.getFailureReason());
        }

        @Test
        @DisplayName("Missing emotion should fail")
        void missingEmotion_shouldFail() {
            String missingEmotionResponse = """
                    {
                      "emotions": {
                        "happiness": {"percentage": 60, "confidence": 0.9},
                        "sadness": {"percentage": 40, "confidence": 0.8}
                      },
                      "dominantEmotion": "happiness",
                      "overallConfidence": 0.85,
                      "reasoning": "Test"
                    }
                    """;

            ValidationResult result = validator.validateEmotionBreakdown(missingEmotionResponse);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("Missing emotions"));
        }

        @Test
        @DisplayName("Percentages not summing to 100 should fail")
        void invalidPercentageSum_shouldFail() {
            String invalidSumResponse = """
                    {
                      "emotions": {
                        "happiness": {"percentage": 50, "confidence": 0.9},
                        "sadness": {"percentage": 20, "confidence": 0.8},
                        "anger": {"percentage": 10, "confidence": 0.7},
                        "fear": {"percentage": 5, "confidence": 0.6},
                        "surprise": {"percentage": 5, "confidence": 0.5},
                        "disgust": {"percentage": 5, "confidence": 0.5},
                        "contempt": {"percentage": 50, "confidence": 0.5}
                      },
                      "dominantEmotion": "happiness",
                      "overallConfidence": 0.85,
                      "reasoning": "Test"
                    }
                    """;

            ValidationResult result = validator.validateEmotionBreakdown(invalidSumResponse);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("sum to"));
        }

        @Test
        @DisplayName("Invalid dominant emotion should fail")
        void invalidDominantEmotion_shouldFail() {
            String invalidDominantResponse = """
                    {
                      "emotions": {
                        "happiness": {"percentage": 50, "confidence": 0.9},
                        "sadness": {"percentage": 20, "confidence": 0.8},
                        "anger": {"percentage": 10, "confidence": 0.7},
                        "fear": {"percentage": 5, "confidence": 0.6},
                        "surprise": {"percentage": 5, "confidence": 0.5},
                        "disgust": {"percentage": 5, "confidence": 0.5},
                        "contempt": {"percentage": 5, "confidence": 0.5}
                      },
                      "dominantEmotion": "joy",
                      "overallConfidence": 0.85,
                      "reasoning": "Test"
                    }
                    """;

            ValidationResult result = validator.validateEmotionBreakdown(invalidDominantResponse);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("Invalid dominant emotion"));
        }

        @Test
        @DisplayName("Empty response should fail")
        void emptyResponse_shouldFail() {
            ValidationResult result = validator.validateEmotionBreakdown("");

            assertFalse(result.isValid());
            assertEquals("Empty response", result.getFailureReason());
        }

        @Test
        @DisplayName("Malformed JSON should fail")
        void malformedJson_shouldFail() {
            String malformed = "{not valid json";

            ValidationResult result = validator.validateEmotionBreakdown(malformed);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("JSON parse error"));
        }
    }

    // ========================================================================
    // RISK ASSESSMENT VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Risk Assessment Validation")
    class RiskAssessmentTests {

        @Test
        @DisplayName("Valid risk assessment should pass")
        void validRiskAssessment_shouldPass() {
            String validResponse = """
                    {
                      "riskScore": 7,
                      "riskLevel": "HIGH",
                      "confidence": 0.8,
                      "crisisIndicators": ["feel alone", "no point"],
                      "reasoning": "Test response"
                    }
                    """;

            ValidationResult result = validator.validateRiskAssessment(validResponse);

            assertTrue(result.isValid());
            assertEquals(0.8, result.getConfidence(), 0.01);
        }

        @Test
        @DisplayName("Invalid risk score should fail")
        void invalidRiskScore_shouldFail() {
            String invalidScore = """
                    {
                      "riskScore": 15,
                      "riskLevel": "HIGH",
                      "confidence": 0.8,
                      "crisisIndicators": [],
                      "reasoning": "Test"
                    }
                    """;

            ValidationResult result = validator.validateRiskAssessment(invalidScore);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("Invalid risk score"));
        }

        @Test
        @DisplayName("Invalid risk level should fail")
        void invalidRiskLevel_shouldFail() {
            String invalidLevel = """
                    {
                      "riskScore": 5,
                      "riskLevel": "DANGER",
                      "confidence": 0.8,
                      "crisisIndicators": [],
                      "reasoning": "Test"
                    }
                    """;

            ValidationResult result = validator.validateRiskAssessment(invalidLevel);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("Invalid risk level"));
        }
    }

    // ========================================================================
    // MOOD SUGGESTION VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Mood Suggestion Validation")
    class MoodSuggestionTests {

        @Test
        @DisplayName("Valid mood suggestion should pass")
        void validMoodSuggestion_shouldPass() {
            String validResponse = """
                    {
                      "emotion": "gratitude",
                      "category": "HAPPY",
                      "intensity": 8,
                      "confidence": 0.85
                    }
                    """;

            ValidationResult result = validator.validateMoodSuggestion(validResponse);

            assertTrue(result.isValid());
            assertEquals(0.85, result.getConfidence(), 0.01);
        }

        @Test
        @DisplayName("Invalid category should fail")
        void invalidCategory_shouldFail() {
            String invalidCategory = """
                    {
                      "emotion": "gratitude",
                      "category": "JOYFUL",
                      "intensity": 8,
                      "confidence": 0.85
                    }
                    """;

            ValidationResult result = validator.validateMoodSuggestion(invalidCategory);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("Invalid mood category"));
        }

        @Test
        @DisplayName("Invalid intensity should fail")
        void invalidIntensity_shouldFail() {
            String invalidIntensity = """
                    {
                      "emotion": "gratitude",
                      "category": "HAPPY",
                      "intensity": 15,
                      "confidence": 0.85
                    }
                    """;

            ValidationResult result = validator.validateMoodSuggestion(invalidIntensity);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("Invalid intensity"));
        }
    }

    // ========================================================================
    // DAILY QUOTE VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Daily Quote Validation")
    class DailyQuoteTests {

        @Test
        @DisplayName("Valid quote should pass")
        void validQuote_shouldPass() {
            String validResponse = """
                    {
                      "quote": "The only way out is through.",
                      "author": "Robert Frost",
                      "verified": true
                    }
                    """;

            ValidationResult result = validator.validateDailyQuote(validResponse);

            assertTrue(result.isValid());
            assertEquals(0.9, result.getConfidence(), 0.01);
        }

        @Test
        @DisplayName("Unverified quote should have lower confidence")
        void unverifiedQuote_shouldHaveLowerConfidence() {
            String unverifiedResponse = """
                    {
                      "quote": "Some wise words.",
                      "author": "Unknown",
                      "verified": false
                    }
                    """;

            ValidationResult result = validator.validateDailyQuote(unverifiedResponse);

            assertTrue(result.isValid());
            assertEquals(0.6, result.getConfidence(), 0.01);
        }

        @Test
        @DisplayName("Missing author should fail")
        void missingAuthor_shouldFail() {
            String missingAuthor = """
                    {
                      "quote": "Some wise words."
                    }
                    """;

            ValidationResult result = validator.validateDailyQuote(missingAuthor);

            assertFalse(result.isValid());
            assertTrue(result.getFailureReason().contains("Missing quote or author"));
        }
    }

    // ========================================================================
    // FALLBACK LOGIC TESTS
    // ========================================================================

    @Nested
    @DisplayName("Fallback Logic")
    class FallbackTests {

        @Test
        @DisplayName("Low confidence should trigger fallback")
        void lowConfidence_shouldTriggerFallback() {
            assertTrue(validator.shouldFallbackToLexicon(0.3));
            assertTrue(validator.shouldFallbackToLexicon(0.49));
        }

        @Test
        @DisplayName("High confidence should not trigger fallback")
        void highConfidence_shouldNotTriggerFallback() {
            assertFalse(validator.shouldFallbackToLexicon(0.5));
            assertFalse(validator.shouldFallbackToLexicon(0.8));
            assertFalse(validator.shouldFallbackToLexicon(1.0));
        }

        @Test
        @DisplayName("Threshold boundary should not trigger fallback")
        void thresholdBoundary_shouldNotTriggerFallback() {
            assertFalse(validator.shouldFallbackToLexicon(AIResponseValidator.CONFIDENCE_THRESHOLD));
        }
    }
}
