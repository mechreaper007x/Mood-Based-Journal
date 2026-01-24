package com.example.moodjournal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Ensemble Risk Service - Combines multiple risk detection layers
 * for robust crisis identification.
 * 
 * Architecture:
 * 1. Layer 1 (Fast): VAD Lexicon - Local keyword matching, no AI
 * 2. Layer 2 (Deep): Gemini AI - Semantic understanding, context-aware
 * 3. Ensemble: Takes MAX(Layer1, Layer2) for safety-first approach
 */
@Service
public class EnsembleRiskService {

    private static final Logger logger = LoggerFactory.getLogger(EnsembleRiskService.class);

    private final VADLexiconService vadLexiconService;

    // Risk thresholds
    public static final int HIGH_RISK_THRESHOLD = 7;
    public static final int MEDIUM_RISK_THRESHOLD = 4;
    public static final int CRISIS_THRESHOLD = 9;

    public EnsembleRiskService(VADLexiconService vadLexiconService) {
        this.vadLexiconService = vadLexiconService;
    }

    /**
     * Performs full ensemble risk analysis on journal entry content.
     * 
     * @param content     The journal entry text
     * @param aiRiskScore Risk score from AI analysis (0-10), -1 if unavailable
     * @param aiVadScores VAD scores from AI (may be null)
     * @return EnsembleResult containing final risk score and metadata
     */
    public EnsembleResult analyzeRisk(String content, int aiRiskScore, Map<String, Double> aiVadScores) {

        // Layer 1: Local lexicon-based analysis
        int lexiconRiskScore = vadLexiconService.calculateRiskScore(content);
        Map<String, Double> lexiconVad = vadLexiconService.analyzeText(content);
        List<String> detectedKeywords = vadLexiconService.detectCrisisKeywords(content);
        int matchedWords = vadLexiconService.getMatchedWordCount(content);

        // Determine final risk score (safety-first: take maximum)
        int finalRiskScore;
        String riskSource;

        if (aiRiskScore >= 0) {
            finalRiskScore = Math.max(lexiconRiskScore, aiRiskScore);
            riskSource = (lexiconRiskScore > aiRiskScore) ? "LEXICON"
                    : (aiRiskScore > lexiconRiskScore) ? "AI" : "BOTH";
        } else {
            // AI unavailable, rely on lexicon only
            finalRiskScore = lexiconRiskScore;
            riskSource = "LEXICON_ONLY";
        }

        // Determine final VAD scores (prefer AI if available and lexicon had few
        // matches)
        Map<String, Double> finalVad;
        if (aiVadScores != null && matchedWords < 3) {
            // Few lexicon matches, trust AI more
            finalVad = aiVadScores;
        } else if (aiVadScores != null && matchedWords >= 3) {
            // Both available, average them
            finalVad = averageVad(lexiconVad, aiVadScores);
        } else {
            finalVad = lexiconVad;
        }

        // Determine risk level
        RiskLevel riskLevel;
        if (finalRiskScore >= CRISIS_THRESHOLD) {
            riskLevel = RiskLevel.CRISIS;
        } else if (finalRiskScore >= HIGH_RISK_THRESHOLD) {
            riskLevel = RiskLevel.HIGH;
        } else if (finalRiskScore >= MEDIUM_RISK_THRESHOLD) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.LOW;
        }

        EnsembleResult result = new EnsembleResult();
        result.finalRiskScore = finalRiskScore;
        result.lexiconRiskScore = lexiconRiskScore;
        result.aiRiskScore = aiRiskScore;
        result.riskSource = riskSource;
        result.riskLevel = riskLevel;
        result.finalVad = finalVad;
        result.lexiconVad = lexiconVad;
        result.aiVad = aiVadScores;
        result.detectedCrisisKeywords = detectedKeywords;
        result.lexiconMatchCount = matchedWords;
        result.requiresImmediateAttention = (riskLevel == RiskLevel.CRISIS || riskLevel == RiskLevel.HIGH);

        logger.info("Ensemble risk analysis: final={} (lexicon={}, ai={}) source={} level={} keywords={}",
                finalRiskScore, lexiconRiskScore, aiRiskScore, riskSource, riskLevel, detectedKeywords);

        return result;
    }

    /**
     * Quick pre-screening using only lexicon (for real-time feedback).
     */
    public QuickScreenResult quickScreen(String content) {
        int riskScore = vadLexiconService.calculateRiskScore(content);
        List<String> keywords = vadLexiconService.detectCrisisKeywords(content);

        QuickScreenResult result = new QuickScreenResult();
        result.riskScore = riskScore;
        result.isHighRisk = riskScore >= HIGH_RISK_THRESHOLD;
        result.isCrisis = riskScore >= CRISIS_THRESHOLD;
        result.detectedKeywords = keywords;

        return result;
    }

    private Map<String, Double> averageVad(Map<String, Double> vad1, Map<String, Double> vad2) {
        return Map.of(
                "valence", (vad1.getOrDefault("valence", 0.5) + vad2.getOrDefault("valence", 0.5)) / 2,
                "arousal", (vad1.getOrDefault("arousal", 0.5) + vad2.getOrDefault("arousal", 0.5)) / 2,
                "dominance", (vad1.getOrDefault("dominance", 0.5) + vad2.getOrDefault("dominance", 0.5)) / 2);
    }

    // Result classes
    public static class EnsembleResult {
        public int finalRiskScore;
        public int lexiconRiskScore;
        public int aiRiskScore;
        public String riskSource;
        public RiskLevel riskLevel;
        public Map<String, Double> finalVad;
        public Map<String, Double> lexiconVad;
        public Map<String, Double> aiVad;
        public List<String> detectedCrisisKeywords;
        public int lexiconMatchCount;
        public boolean requiresImmediateAttention;
    }

    public static class QuickScreenResult {
        public int riskScore;
        public boolean isHighRisk;
        public boolean isCrisis;
        public List<String> detectedKeywords;
    }

    public enum RiskLevel {
        LOW, // 0-3
        MEDIUM, // 4-6
        HIGH, // 7-8
        CRISIS // 9-10
    }
}
