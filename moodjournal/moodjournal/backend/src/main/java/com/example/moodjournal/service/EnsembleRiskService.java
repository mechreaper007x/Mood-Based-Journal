package com.example.moodjournal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ensemble Risk Service - Combines multiple risk detection layers
 * for robust crisis identification with hallucination mitigation.
 * 
 * Architecture:
 * 1. Layer 1 (Fast): VAD Lexicon - Local keyword matching, no AI
 * 2. Layer 2 (Deep): Gemini AI - Semantic understanding, context-aware
 * 3. Ensemble: Confidence-weighted voting with safety-first MAX approach
 * 
 * Hallucination Guardrails:
 * - Confidence-weighted voting when AI confidence is low
 * - Discrepancy logging for audit and improvement
 * - Circuit breaker for persistent AI failures
 */
@Service
public class EnsembleRiskService {

    private static final Logger logger = LoggerFactory.getLogger(EnsembleRiskService.class);

    private final VADLexiconService vadLexiconService;

    // Risk thresholds
    public static final int HIGH_RISK_THRESHOLD = 7;
    public static final int MEDIUM_RISK_THRESHOLD = 4;
    public static final int CRISIS_THRESHOLD = 9;

    // Confidence threshold below which we weight lexicon more heavily
    private static final double LOW_CONFIDENCE_THRESHOLD = 0.5;

    // Discrepancy threshold (if AI and lexicon differ by more than this, log for
    // audit)
    private static final int DISCREPANCY_THRESHOLD = 3;

    // Circuit breaker: consecutive failures before disabling AI
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private final AtomicInteger consecutiveAIFailures = new AtomicInteger(0);
    private final AtomicBoolean aiCircuitBreakerOpen = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicLong lastFailureTime = new java.util.concurrent.atomic.AtomicLong(
            0);

    // 5 minutes cooldown
    private static final long CIRCUIT_BREAKER_COOLDOWN_MS = 5 * 60 * 1000;

    public EnsembleRiskService(VADLexiconService vadLexiconService) {
        this.vadLexiconService = vadLexiconService;
    }

    /**
     * Performs full ensemble risk analysis with confidence-weighted voting.
     * 
     * @param content      The journal entry text
     * @param aiRiskScore  Risk score from AI analysis (0-10), -1 if unavailable
     * @param aiConfidence AI's reported confidence (0.0-1.0)
     * @param aiVadScores  VAD scores from AI (may be null)
     * @return EnsembleResult containing final risk score and metadata
     */
    public EnsembleResult analyzeRisk(String content, int aiRiskScore, double aiConfidence,
            Map<String, Double> aiVadScores) {

        // Layer 1: Local lexicon-based analysis (always available)
        int lexiconRiskScore = vadLexiconService.calculateRiskScore(content);
        Map<String, Double> lexiconVad = vadLexiconService.analyzeText(content);
        List<String> detectedKeywords = vadLexiconService.detectCrisisKeywords(content);
        int matchedWords = vadLexiconService.getMatchedWordCount(content);

        // Check circuit breaker with Half-Open recovery logic
        if (aiCircuitBreakerOpen.get()) {
            // V10 FIX: Auto-recovery check (Half-Open State)
            long lastFailure = lastFailureTime.get();
            if (System.currentTimeMillis() - lastFailure > CIRCUIT_BREAKER_COOLDOWN_MS) {
                // Allow ONE trial request through (Half-Open)
                logger.info("Circuit breaker attempting recovery (Half-Open)...");
            } else {
                logger.warn("AI circuit breaker OPEN - using lexicon only. Next retry in {}ms",
                        CIRCUIT_BREAKER_COOLDOWN_MS - (System.currentTimeMillis() - lastFailure));
                return buildLexiconOnlyResult(lexiconRiskScore, lexiconVad, detectedKeywords, matchedWords);
            }
        }

        // Determine final risk score using confidence-weighted voting
        int finalRiskScore;
        String riskSource;
        boolean discrepancyDetected = false;

        if (aiRiskScore >= 0) {
            // Reset circuit breaker on successful AI response
            consecutiveAIFailures.set(0);

            // Check for discrepancy
            int discrepancy = Math.abs(lexiconRiskScore - aiRiskScore);
            if (discrepancy > DISCREPANCY_THRESHOLD) {
                discrepancyDetected = true;
                logDiscrepancy(content, lexiconRiskScore, aiRiskScore, aiConfidence, discrepancy);
            }

            // Confidence-weighted voting
            if (aiConfidence < LOW_CONFIDENCE_THRESHOLD) {
                // Low AI confidence: weight lexicon at 80%, AI at 20%
                // V4 Fix: Ensure bounded calculation
                double weightedScore = lexiconRiskScore * 0.8 + aiRiskScore * 0.2;
                finalRiskScore = (int) Math.round(Math.max(0, Math.min(10, weightedScore)));
                riskSource = "WEIGHTED_LEXICON";
                logger.info("Low AI confidence ({:.2f}), using weighted scoring: lexicon*0.8 + ai*0.2",
                        aiConfidence);
            } else {
                // Normal confidence: safety-first MAX approach
                finalRiskScore = Math.max(lexiconRiskScore, aiRiskScore);
                riskSource = (lexiconRiskScore > aiRiskScore) ? "LEXICON"
                        : (aiRiskScore > lexiconRiskScore) ? "AI" : "BOTH";
            }
        } else {
            // AI unavailable - increment failure counter
            int failures = consecutiveAIFailures.incrementAndGet();
            if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
                // V10 Fix: Atomic transition
                if (aiCircuitBreakerOpen.compareAndSet(false, true)) {
                    lastFailureTime.set(System.currentTimeMillis());
                    logger.error("AI circuit breaker TRIPPED after {} consecutive failures", failures);
                }
            }

            finalRiskScore = lexiconRiskScore;
            riskSource = "LEXICON_ONLY";
        }

        // Determine final VAD scores
        Map<String, Double> finalVad = determineVadScores(
                lexiconVad, aiVadScores, matchedWords, aiConfidence);

        // Determine risk level
        RiskLevel riskLevel = mapScoreToRiskLevel(finalRiskScore);

        // Build result
        EnsembleResult result = new EnsembleResult();
        result.finalRiskScore = finalRiskScore;
        result.lexiconRiskScore = lexiconRiskScore;
        result.aiRiskScore = aiRiskScore;
        result.aiConfidence = aiConfidence;
        result.riskSource = riskSource;
        result.riskLevel = riskLevel;
        result.finalVad = finalVad;
        result.lexiconVad = lexiconVad;
        result.aiVad = aiVadScores;
        result.detectedCrisisKeywords = detectedKeywords;
        result.lexiconMatchCount = matchedWords;
        result.requiresImmediateAttention = (riskLevel == RiskLevel.CRISIS || riskLevel == RiskLevel.HIGH);
        result.discrepancyDetected = discrepancyDetected;

        logger.info("Ensemble risk: final={} (lexicon={}, ai={}, conf={:.2f}) source={} level={} keywords={}",
                finalRiskScore, lexiconRiskScore, aiRiskScore, aiConfidence, riskSource, riskLevel, detectedKeywords);

        return result;
    }

    /**
     * Overload for backward compatibility (no confidence parameter).
     */
    public EnsembleResult analyzeRisk(String content, int aiRiskScore, Map<String, Double> aiVadScores) {
        // Default to medium confidence if not provided
        return analyzeRisk(content, aiRiskScore, 0.7, aiVadScores);
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

    /**
     * Reset the circuit breaker (for testing or manual reset).
     */
    public void resetCircuitBreaker() {
        aiCircuitBreakerOpen.set(false);
        consecutiveAIFailures.set(0);
        logger.info("AI circuit breaker reset");
    }

    /**
     * Check if circuit breaker is open.
     */
    public boolean isCircuitBreakerOpen() {
        return aiCircuitBreakerOpen.get();
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    private EnsembleResult buildLexiconOnlyResult(int riskScore, Map<String, Double> vad,
            List<String> keywords, int matchCount) {
        EnsembleResult result = new EnsembleResult();
        result.finalRiskScore = riskScore;
        result.lexiconRiskScore = riskScore;
        result.aiRiskScore = -1;
        result.aiConfidence = 0.0;
        result.riskSource = "LEXICON_ONLY_CIRCUIT_BREAKER";
        result.riskLevel = mapScoreToRiskLevel(riskScore);
        result.finalVad = vad;
        result.lexiconVad = vad;
        result.aiVad = null;
        result.detectedCrisisKeywords = keywords;
        result.lexiconMatchCount = matchCount;
        result.requiresImmediateAttention = (result.riskLevel == RiskLevel.CRISIS ||
                result.riskLevel == RiskLevel.HIGH);
        result.discrepancyDetected = false;
        return result;
    }

    private Map<String, Double> determineVadScores(Map<String, Double> lexiconVad,
            Map<String, Double> aiVad,
            int matchedWords, double aiConfidence) {
        if (aiVad == null) {
            return lexiconVad;
        }

        if (matchedWords < 3 && aiConfidence >= LOW_CONFIDENCE_THRESHOLD) {
            // Few lexicon matches + high AI confidence = trust AI
            return aiVad;
        } else if (matchedWords >= 3) {
            // Both available - average them
            return averageVad(lexiconVad, aiVad);
        }

        return lexiconVad;
    }

    private Map<String, Double> averageVad(Map<String, Double> vad1, Map<String, Double> vad2) {
        return Map.of(
                "valence", (vad1.getOrDefault("valence", 0.5) + vad2.getOrDefault("valence", 0.5)) / 2,
                "arousal", (vad1.getOrDefault("arousal", 0.5) + vad2.getOrDefault("arousal", 0.5)) / 2,
                "dominance", (vad1.getOrDefault("dominance", 0.5) + vad2.getOrDefault("dominance", 0.5)) / 2);
    }

    private RiskLevel mapScoreToRiskLevel(int score) {
        if (score >= CRISIS_THRESHOLD)
            return RiskLevel.CRISIS;
        if (score >= HIGH_RISK_THRESHOLD)
            return RiskLevel.HIGH;
        if (score >= MEDIUM_RISK_THRESHOLD)
            return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private void logDiscrepancy(String content, int lexiconScore, int aiScore,
            double aiConfidence, int discrepancy) {
        // Truncate content for logging (privacy + log size)
        String truncatedContent = content.length() > 100
                ? content.substring(0, 100) + "..."
                : content;

        logger.warn("DISCREPANCY DETECTED: lexicon={}, ai={}, confidence={:.2f}, diff={}, content='{}'",
                lexiconScore, aiScore, aiConfidence, discrepancy,
                truncatedContent.replaceAll("[\\r\\n]", " "));

        // TODO: In production, consider persisting these for model improvement
    }

    // ========================================================================
    // RESULT CLASSES
    // ========================================================================

    public static class EnsembleResult {
        public int finalRiskScore;
        public int lexiconRiskScore;
        public int aiRiskScore;
        public double aiConfidence;
        public String riskSource;
        public RiskLevel riskLevel;
        public Map<String, Double> finalVad;
        public Map<String, Double> lexiconVad;
        public Map<String, Double> aiVad;
        public List<String> detectedCrisisKeywords;
        public int lexiconMatchCount;
        public boolean requiresImmediateAttention;
        public boolean discrepancyDetected;
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
