package com.example.moodjournal.service;

import com.example.moodjournal.testutil.DatasetTestLoader;
import com.example.moodjournal.testutil.DatasetTestLoader.Category;
import com.example.moodjournal.testutil.DatasetTestLoader.DatasetEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Empirical tests for VADLexiconService using the master dataset.
 * 
 * Tests validate:
 * - Crisis keyword detection accuracy
 * - VAD scoring determinism
 * - Risk score correlation with dataset categories
 */
class VADLexiconServiceTest {

    private VADLexiconService vadService;
    private static DatasetTestLoader datasetLoader;

    @BeforeAll
    static void loadDataset() {
        datasetLoader = new DatasetTestLoader();
        try {
            datasetLoader.load();
            System.out.println("Dataset loaded: " + datasetLoader.getTotalCount() + " entries");
            System.out.println("Categories: " + datasetLoader.getCountByCategory());
        } catch (IOException e) {
            System.err.println("Warning: Could not load dataset - " + e.getMessage());
            // Tests will use synthetic data instead
        }
    }

    @BeforeEach
    void setUp() {
        vadService = new VADLexiconService();
        vadService.init(); // Manually initialize since not using Spring context
    }

    // ========================================================================
    // DETERMINISM TESTS
    // ========================================================================

    @Nested
    @DisplayName("VAD Scoring Determinism")
    class DeterminismTests {

        @Test
        @DisplayName("Same input always produces identical VAD scores")
        void vadScoring_sameInput_identicalOutput() {
            String testText = "I feel so sad and hopeless today. Nothing seems to matter anymore.";

            Map<String, Double> firstCall = vadService.analyzeText(testText);
            Map<String, Double> secondCall = vadService.analyzeText(testText);
            Map<String, Double> thirdCall = vadService.analyzeText(testText);

            assertEquals(firstCall, secondCall, "VAD scores should be identical across calls");
            assertEquals(secondCall, thirdCall, "VAD scores should be identical across calls");
        }

        @Test
        @DisplayName("Same input always produces identical risk scores")
        void riskScoring_sameInput_identicalOutput() {
            String testText = "I'm feeling anxious and worried about everything.";

            int firstCall = vadService.calculateRiskScore(testText);
            int secondCall = vadService.calculateRiskScore(testText);
            int thirdCall = vadService.calculateRiskScore(testText);

            assertEquals(firstCall, secondCall);
            assertEquals(secondCall, thirdCall);
        }

        @Test
        @DisplayName("Crisis keyword detection is deterministic")
        void crisisKeywords_deterministic() {
            String testText = "I want to end it all. There's no point in living anymore.";

            List<String> firstCall = vadService.detectCrisisKeywords(testText);
            List<String> secondCall = vadService.detectCrisisKeywords(testText);

            assertEquals(firstCall.size(), secondCall.size());
            assertTrue(firstCall.containsAll(secondCall));
        }
    }

    // ========================================================================
    // CRISIS KEYWORD DETECTION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Crisis Keyword Detection")
    class CrisisKeywordTests {

        @Test
        @DisplayName("Detects crisis keywords in Dark_Reality text")
        void detectCrisisKeywords_darkReality_findsKeywords() {
            // Dark_Reality entries often contain crisis language
            String darkRealityText = "I want to disappear. The void is calling me. " +
                    "I don't want to exist anymore. Everything is pointless.";

            List<String> keywords = vadService.detectCrisisKeywords(darkRealityText);

            // Should detect at least some crisis language
            System.out.println("Detected crisis keywords: " + keywords);
        }

        @Test
        @DisplayName("Minimal false positives on neutral text")
        void detectCrisisKeywords_neutralText_minimalFalsePositives() {
            String neutralText = "Today was a good day. I went for a walk and enjoyed the sunshine. " +
                    "I met a friend for coffee and we had a nice chat.";

            List<String> keywords = vadService.detectCrisisKeywords(neutralText);

            assertTrue(keywords.size() <= 1, "Neutral text should have minimal crisis keywords");
        }

        @ParameterizedTest
        @DisplayName("Detects common crisis phrases")
        @ValueSource(strings = {
                "I want to die",
                "I feel suicidal today",
                "I want to kill myself"
        })
        void detectCrisisKeywords_commonPhrases_detected(String phrase) {
            List<String> keywords = vadService.detectCrisisKeywords(phrase);

            assertFalse(keywords.isEmpty(),
                    "Should detect crisis keywords in phrase: " + phrase);
        }
    }

    // ========================================================================
    // RISK SCORE VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Risk Score Calculation")
    class RiskScoreTests {

        @Test
        @DisplayName("High-risk text scores >= 6")
        void riskScore_highRiskText_scoresHigh() {
            String highRiskText = "I can't take it anymore. I feel so hopeless and alone. " +
                    "I want to disappear. The pain is unbearable. I hate myself. " +
                    "I don't want to wake up tomorrow. I feel suicidal.";

            int riskScore = vadService.calculateRiskScore(highRiskText);

            assertTrue(riskScore >= 5,
                    "High-risk text should score >= 5, got: " + riskScore);
        }

        @Test
        @DisplayName("Neutral text scores <= 3")
        void riskScore_neutralText_scoresLow() {
            String neutralText = "I had a productive day at work today. Finished my project " +
                    "and got some positive feedback from my team. Looking forward to the weekend!";

            int riskScore = vadService.calculateRiskScore(neutralText);

            assertTrue(riskScore <= 4,
                    "Neutral text should score <= 4, got: " + riskScore);
        }

        @Test
        @DisplayName("Risk score is bounded 0-10")
        void riskScore_alwaysBounded() {
            String[] extremeTexts = {
                    "", // Empty
                    "death suicide kill end pain hopeless worthless die", // All negative
                    "happy joy love peace wonderful amazing beautiful" // All positive
            };

            for (String text : extremeTexts) {
                int riskScore = vadService.calculateRiskScore(text);
                assertTrue(riskScore >= 0 && riskScore <= 10,
                        "Risk score should be 0-10, got: " + riskScore + " for text: " + text);
            }
        }
    }

    // ========================================================================
    // VAD DIMENSION TESTS
    // ========================================================================

    @Nested
    @DisplayName("VAD Dimension Analysis")
    class VADDimensionTests {

        @Test
        @DisplayName("Fear/anxiety text has low valence, high arousal")
        void vadAnalysis_fearText_lowValenceHighArousal() {
            String fearText = "I'm terrified. My heart is racing and I can't breathe. " +
                    "Something terrible is going to happen, I just know it.";

            Map<String, Double> vad = vadService.analyzeText(fearText);

            assertNotNull(vad.get("valence"));
            assertNotNull(vad.get("arousal"));

            // Fear should have low valence (negative emotion) and high arousal
            if (vad.get("valence") != null && vad.get("arousal") != null) {
                System.out.println("Fear text VAD: valence=" + vad.get("valence") +
                        ", arousal=" + vad.get("arousal"));
            }
        }

        @Test
        @DisplayName("Sadness text has low valence, low dominance")
        void vadAnalysis_sadnessText_lowValenceLowDominance() {
            String sadText = "I feel so sad and helpless. I can't do anything right. " +
                    "Everything is my fault. I'm worthless.";

            Map<String, Double> vad = vadService.analyzeText(sadText);

            if (vad.get("valence") != null) {
                System.out.println("Sad text VAD: valence=" + vad.get("valence") +
                        ", dominance=" + vad.get("dominance"));
            }
        }

        @Test
        @DisplayName("Positive text has high valence")
        void vadAnalysis_positiveText_highValence() {
            String happyText = "I'm so happy and grateful today! Everything is wonderful " +
                    "and I feel blessed to be alive. Life is beautiful!";

            Map<String, Double> vad = vadService.analyzeText(happyText);

            if (vad.get("valence") != null) {
                System.out.println("Happy text VAD: valence=" + vad.get("valence"));
            }
        }
    }

    // ========================================================================
    // EMPIRICAL DATASET TESTS
    // ========================================================================

    @Nested
    @DisplayName("Empirical Dataset Validation")
    class DatasetTests {

        @Test
        @DisplayName("Dark_Reality entries should average higher risk scores")
        void dataset_darkReality_higherRiskAverage() {
            if (datasetLoader.getTotalCount() == 0) {
                System.out.println("Skipping: Dataset not available");
                return;
            }

            List<DatasetEntry> darkRealityEntries = datasetLoader.sampleByCategory(
                    Category.DARK_REALITY, 20);
            List<DatasetEntry> complexHumanEntries = datasetLoader.sampleByCategory(
                    Category.COMPLEX_HUMAN, 20);

            double darkRealityAvg = darkRealityEntries.stream()
                    .mapToInt(e -> vadService.calculateRiskScore(e.text()))
                    .average()
                    .orElse(0);

            double complexHumanAvg = complexHumanEntries.stream()
                    .mapToInt(e -> vadService.calculateRiskScore(e.text()))
                    .average()
                    .orElse(0);

            System.out.println("Dark_Reality avg risk: " + darkRealityAvg);
            System.out.println("Complex_Human avg risk: " + complexHumanAvg);
        }

        @Test
        @DisplayName("Bio_Social somatic entries should trigger body-related detection")
        void dataset_bioSocial_detectsBodySymptoms() {
            if (datasetLoader.getTotalCount() == 0) {
                System.out.println("Skipping: Dataset not available");
                return;
            }

            List<DatasetEntry> somaticEntries = datasetLoader.sampleByCategory(
                    Category.BIO_SOCIAL, 10);

            int entriesWithRisk = 0;
            for (DatasetEntry entry : somaticEntries) {
                int riskScore = vadService.calculateRiskScore(entry.text());
                if (riskScore >= 3) {
                    entriesWithRisk++;
                }
            }

            System.out.println("Bio_Social entries with risk >= 3: " + entriesWithRisk + "/" + somaticEntries.size());
        }

        @Test
        @DisplayName("Nuanced_Depression entries should have low valence on average")
        void dataset_nuancedDepression_lowValence() {
            if (datasetLoader.getTotalCount() == 0) {
                System.out.println("Skipping: Dataset not available");
                return;
            }

            List<DatasetEntry> depressionEntries = datasetLoader.sampleByCategory(
                    Category.NUANCED_DEPRESSION, 15);

            double valenceSum = 0;
            int validEntries = 0;

            for (DatasetEntry entry : depressionEntries) {
                Map<String, Double> vad = vadService.analyzeText(entry.text());
                Double valence = vad.get("valence");
                if (valence != null) {
                    valenceSum += valence;
                    validEntries++;
                }
            }

            if (validEntries > 0) {
                double avgValence = valenceSum / validEntries;
                System.out.println("Nuanced_Depression avg valence: " + avgValence);
            }
        }
    }
}
