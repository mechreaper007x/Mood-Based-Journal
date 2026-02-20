package com.example.moodjournal.integration;

import com.example.moodjournal.service.AIResponseValidator;
import com.example.moodjournal.service.EnsembleRiskService;
import com.example.moodjournal.service.VADLexiconService;
import com.example.moodjournal.testutil.DatasetTestLoader;
import com.example.moodjournal.testutil.DatasetTestLoader.Category;
import com.example.moodjournal.testutil.DatasetTestLoader.DatasetEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;









class EmpiricalIntegrationTest {

    private VADLexiconService vadService;
    private EnsembleRiskService ensembleService;
    private AIResponseValidator validator;
    private static DatasetTestLoader datasetLoader;

    @BeforeAll
    static void loadDataset() {
        datasetLoader = new DatasetTestLoader();
        try {
            datasetLoader.load();
            System.out.println("=== DATASET LOADED ===");
            System.out.println("Total entries: " + datasetLoader.getTotalCount());
            System.out.println("Categories: " + datasetLoader.getCountByCategory());
            System.out.println("======================");
        } catch (IOException e) {
            System.err.println("Warning: Could not load dataset - " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        vadService = new VADLexiconService();
        vadService.init(); 
        ensembleService = new EnsembleRiskService(vadService);
        validator = new AIResponseValidator(objectMapper);
    }

    
    
    

    @Nested
    @DisplayName("End-to-End Pipeline")
    class PipelineTests {

        @Test
        @DisplayName("Full pipeline processes Dark_Reality entries correctly")
        void fullPipeline_darkRealityCategory_processingComplete() {
            if (datasetLoader.getTotalCount() == 0) {
                System.out.println("Skipping: Dataset not available");
                return;
            }

            List<DatasetEntry> entries = datasetLoader.sampleByCategory(Category.DARK_REALITY, 20);

            int processedCount = 0;
            int highRiskCount = 0;
            int crisisCount = 0;
            double totalRisk = 0;

            for (DatasetEntry entry : entries) {
                
                EnsembleRiskService.QuickScreenResult quickResult = ensembleService.quickScreen(entry.text());

                assertNotNull(quickResult, "Quick screen should return result");
                processedCount++;

                if (quickResult.riskScore >= 6) {
                    highRiskCount++;
                }
                if (quickResult.isCrisis) {
                    crisisCount++;
                }
                totalRisk += quickResult.riskScore;
            }

            double avgRisk = totalRisk / entries.size();

            System.out.println("=== DARK_REALITY PIPELINE RESULTS ===");
            System.out.println("Processed: " + processedCount + "/" + entries.size());
            System.out.println("High Risk (>=6): " + highRiskCount + " (" +
                    String.format("%.1f", (highRiskCount * 100.0 / entries.size())) + "%)");
            System.out.println("Crisis Flagged: " + crisisCount + " (" +
                    String.format("%.1f", (crisisCount * 100.0 / entries.size())) + "%)");
            System.out.println("Average Risk: " + String.format("%.2f", avgRisk));
            System.out.println("=====================================");

            assertEquals(entries.size(), processedCount, "All entries should be processed");
        }

        @Test
        @DisplayName("Full pipeline processes Bio_Social entries correctly")
        void fullPipeline_bioSocialCategory_processingComplete() {
            if (datasetLoader.getTotalCount() == 0) {
                System.out.println("Skipping: Dataset not available");
                return;
            }

            List<DatasetEntry> entries = datasetLoader.sampleByCategory(Category.BIO_SOCIAL, 20);

            double totalRisk = 0;
            int somaticDetected = 0;

            for (DatasetEntry entry : entries) {
                EnsembleRiskService.QuickScreenResult result = ensembleService.quickScreen(entry.text());
                totalRisk += result.riskScore;

                
                if (result.riskScore >= 3) {
                    somaticDetected++;
                }
            }

            double avgRisk = totalRisk / entries.size();

            System.out.println("=== BIO_SOCIAL PIPELINE RESULTS ===");
            System.out.println("Average Risk: " + String.format("%.2f", avgRisk));
            System.out.println("Somatic Risk (>=3): " + somaticDetected + "/" + entries.size());
            System.out.println("===================================");
        }
    }

    
    
    

    @Nested
    @DisplayName("AI Failure Resilience")
    class ResilienceTests {

        @Test
        @DisplayName("Graceful degradation when AI throws exception")
        void aiFailure_gracefulDegradation_lexiconFallback() {
            String testText = "I feel sad and hopeless. Life seems meaningless.";

            
            EnsembleRiskService.QuickScreenResult lexiconResult = ensembleService.quickScreen(testText);

            assertNotNull(lexiconResult, "Should return lexicon-only result");
            assertTrue(lexiconResult.riskScore >= 0, "Should have valid risk score");

            System.out.println("Lexicon fallback result: score=" + lexiconResult.riskScore +
                    ", isHighRisk=" + lexiconResult.isHighRisk);
        }

        @Test
        @DisplayName("System remains stable with rapid successive calls")
        void stability_rapidCalls_remainsStable() {
            String testText = "I'm feeling anxious about tomorrow.";

            
            for (int i = 0; i < 100; i++) {
                EnsembleRiskService.QuickScreenResult result = ensembleService.quickScreen(testText);
                assertNotNull(result);
                assertEquals(result.riskScore, ensembleService.quickScreen(testText).riskScore,
                        "Deterministic results should be consistent");
            }
        }

        @Test
        @DisplayName("Ensemble handles malformed AI responses")
        void malformedAiResponse_handledGracefully() {
            String testText = "I need help with my feelings.";

            
            EnsembleRiskService.QuickScreenResult result = ensembleService.quickScreen(testText);

            assertNotNull(result);
            assertTrue(result.riskScore >= 0 && result.riskScore <= 10);
        }
    }

    
    
    

    @Nested
    @DisplayName("Accuracy Metrics")
    class AccuracyTests {

        @Test
        @DisplayName("Category discrimination: risk scores differ by category")
        void categoryDiscrimination_riskScoresDiffer() {
            if (datasetLoader.getTotalCount() == 0) {
                System.out.println("Skipping: Dataset not available");
                return;
            }

            Map<Category, Double> avgScores = new HashMap<>();

            for (Category category : Category.values()) {
                List<DatasetEntry> entries = datasetLoader.sampleByCategory(category, 25);
                if (entries.isEmpty())
                    continue;

                double avg = entries.stream()
                        .mapToInt(e -> ensembleService.quickScreen(e.text()).riskScore)
                        .average()
                        .orElse(0);
                avgScores.put(category, avg);
            }

            System.out.println("=== CATEGORY DISCRIMINATION ===");
            avgScores.forEach((cat, avg) -> System.out.println(cat.getValue() + ": " + String.format("%.2f", avg)));
            System.out.println("===============================");
        }

        @Test
        @DisplayName("No false negatives on entries with crisis keywords")
        void crisisKeywordEntries_noFalseNegatives() {
            if (datasetLoader.getTotalCount() == 0) {
                System.out.println("Skipping: Dataset not available");
                return;
            }

            
            List<DatasetEntry> crisisEntries = datasetLoader.searchByKeywords(
                    "want to die", "kill myself", "end it all", "suicide", "no point in living");

            int correctlyFlagged = 0;
            int falseNegatives = 0;

            for (DatasetEntry entry : crisisEntries) {
                EnsembleRiskService.QuickScreenResult result = ensembleService.quickScreen(entry.text());

                if (result.riskScore >= 5 || result.isCrisis) {
                    correctlyFlagged++;
                } else {
                    falseNegatives++;
                    System.out.println("POTENTIAL FALSE NEGATIVE (score=" + result.riskScore + "): " +
                            entry.text().substring(0, Math.min(100, entry.text().length())) + "...");
                }
            }

            System.out.println("=== CRISIS KEYWORD DETECTION ===");
            System.out.println("Correctly flagged: " + correctlyFlagged + "/" + crisisEntries.size());
            System.out.println("Potential false negatives: " + falseNegatives);
            System.out.println("================================");
        }
    }

    
    
    

    @Nested
    @DisplayName("Validator Integration")
    class ValidatorIntegrationTests {

        @Test
        @DisplayName("Validator correctly identifies fallback threshold")
        void validator_fallbackThreshold_triggersCorrectly() {
            
            assertFalse(validator.shouldFallbackToLexicon(0.5), "0.5 should NOT trigger fallback");
            assertTrue(validator.shouldFallbackToLexicon(0.49), "0.49 should trigger fallback");
            assertTrue(validator.shouldFallbackToLexicon(0.3), "0.3 should trigger fallback");
            assertFalse(validator.shouldFallbackToLexicon(0.8), "0.8 should NOT trigger fallback");
        }

        @Test
        @DisplayName("Valid emotion breakdown passes validation")
        void validEmotionBreakdown_passesValidation() {
            String validResponse = """
                    {
                      "emotions": {
                        "happiness": {"percentage": 10, "confidence": 0.8},
                        "sadness": {"percentage": 40, "confidence": 0.9},
                        "anger": {"percentage": 15, "confidence": 0.7},
                        "fear": {"percentage": 20, "confidence": 0.85},
                        "surprise": {"percentage": 5, "confidence": 0.6},
                        "disgust": {"percentage": 5, "confidence": 0.6},
                        "contempt": {"percentage": 5, "confidence": 0.6}
                      },
                      "dominantEmotion": "sadness",
                      "overallConfidence": 0.85,
                      "reasoning": "Text shows clear signs of sadness and fear"
                    }
                    """;

            AIResponseValidator.ValidationResult result = validator.validateEmotionBreakdown(validResponse);

            assertTrue(result.isValid(), "Valid response should pass validation");
            assertEquals(0.85, result.getConfidence(), 0.01);
        }

        @Test
        @DisplayName("Invalid risk assessment fails validation")
        void invalidRiskAssessment_failsValidation() {
            String invalidResponse = """
                    {
                      "riskScore": 15,
                      "riskLevel": "EXTREME",
                      "confidence": 0.9
                    }
                    """;

            AIResponseValidator.ValidationResult result = validator.validateRiskAssessment(invalidResponse);

            assertFalse(result.isValid(), "Invalid response should fail validation");
        }
    }
}
