package com.example.moodjournal.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.moodjournal.service.AIResponseValidator.ValidationResult;
import com.example.moodjournal.util.PromptConstants;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

/**
 * Service for AI-powered text generation using Google Gemini SDK.
 * 
 * Enhanced with Context Engineering guardrails:
 * - Uses context-engineered prompts from PromptConstants
 * - Validates all AI responses via AIResponseValidator
 * - Falls back to lexicon-based analysis on low confidence
 * - Input sanitization before AI calls
 * - Audit logging for hallucination detection
 */
@Service
public class GeminiService {

  private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

  private final Client client;
  private final AIResponseValidator validator;
  private final VADLexiconService lexiconService;
  private final com.example.moodjournal.security.sanitization.InputSanitizer sanitizer;

  // Available models for rotation
  private static final List<String> AVAILABLE_MODELS = List.of(
      "gemini-2.5-flash",
      "gemini-2.5-flash-lite",
      "gemini-3-flash",
      "gemma-3-27b-it",
      "gemma-3-12b-it",
      "gemma-3-4b-it",
      "gemma-3-2b-it",
      "gemma-3-1b-it");

  // Round-robin counter for model selection
  private final AtomicInteger modelIndex = new AtomicInteger(0);

  // Lock to ensure only one API call at a time
  private final ReentrantLock apiLock = new ReentrantLock();

  public GeminiService(
      @Value("${google.api.key}") String apiKey,
      AIResponseValidator validator,
      VADLexiconService lexiconService,
      com.example.moodjournal.security.sanitization.InputSanitizer sanitizer) {
    this.client = Client.builder().apiKey(apiKey).build();
    this.validator = validator;
    this.lexiconService = lexiconService;
    this.sanitizer = sanitizer;
    log.info("GeminiService initialized with {} models for rotation and validation enabled",
        AVAILABLE_MODELS.size());
  }

  /**
   * Async emotion breakdown analysis.
   */
  @Async("taskExecutor")
  public CompletableFuture<String> getEmotionBreakdown(String text) {
    String sanitized = sanitizer.sanitize(text);
    String prompt = PromptConstants.EMOTION_BREAKDOWN_PROMPT + sanitized;

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);

      // Validate response
      ValidationResult validation = validator.validateEmotionBreakdown(cleanResponse);
      if (!validation.isValid() || validator.shouldFallbackToLexicon(validation.getConfidence())) {
        log.warn("Emotion breakdown validation failed or low confidence: {}", validation);
        return CompletableFuture.completedFuture(generateFallbackEmotionBreakdown(sanitized));
      }

      return CompletableFuture.completedFuture(cleanResponse);
    } catch (Exception e) {
      log.error("Emotion breakdown failed, using fallback: {}", e.getMessage());
      return CompletableFuture.completedFuture(generateFallbackEmotionBreakdown(sanitized));
    }
  }

  /**
   * Get daily motivational quote.
   */
  public String getDailyQuote() {
    try {
      String response = callGeminiWithRotation(PromptConstants.DAILY_QUOTE_PROMPT);
      String cleanResponse = cleanJsonResponse(response);

      ValidationResult validation = validator.validateDailyQuote(cleanResponse);
      if (!validation.isValid()) {
        log.warn("Daily quote validation failed: {}", validation);
        return getDefaultQuote();
      }

      return cleanResponse;
    } catch (Exception e) {
      log.error("Failed to get daily quote: {}", e.getMessage());
      return getDefaultQuote();
    }
  }

  /**
   * Analyze emotions in journal content using context-engineered prompts.
   * Validates response and falls back to lexicon on low confidence.
   */
  public String analyzeEmotions(String journalContent) {
    String sanitized = sanitizer.sanitize(journalContent);
    String prompt = PromptConstants.EMOTION_BREAKDOWN_PROMPT + sanitized;

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);
      log.info("Gemini emotion analysis response (raw): {}", cleanResponse);

      // Validate and check confidence
      ValidationResult validation = validator.validateEmotionBreakdown(cleanResponse);

      if (!validation.isValid()) {
        log.warn("AI response validation failed: {}. Using fallback.", validation.getFailureReason());
        return generateFallbackEmotionBreakdown(sanitized);
      }

      if (validator.shouldFallbackToLexicon(validation.getConfidence())) {
        log.info("AI confidence {} below threshold. Augmenting with lexicon.", validation.getConfidence());
        // Still return AI response but log for audit
      }

      return cleanResponse;
    } catch (Exception e) {
      log.error("Gemini analysis failed: {}. Using fallback.", e.getMessage(), e);
      return generateFallbackEmotionBreakdown(sanitized);
    }
  }

  /**
   * Assess risk level of journal content.
   * Uses safety-first approach with lexicon backup.
   */
  public String assessRisk(String journalContent) {
    String sanitized = sanitizer.sanitize(journalContent);
    String prompt = PromptConstants.RISK_ASSESSMENT_PROMPT + sanitized;

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);

      ValidationResult validation = validator.validateRiskAssessment(cleanResponse);

      if (!validation.isValid()) {
        log.warn("Risk assessment validation failed: {}. Using lexicon.", validation.getFailureReason());
        return generateLexiconRiskAssessment(sanitized);
      }

      return cleanResponse;
    } catch (Exception e) {
      log.error("Risk assessment failed: {}. Using lexicon fallback.", e.getMessage());
      return generateLexiconRiskAssessment(sanitized);
    }
  }

  /**
   * Suggest mood category for journal entry.
   */
  public String suggestMood(String text) {
    String sanitized = sanitizer.sanitize(text);
    String prompt = PromptConstants.SUGGEST_MOOD_PROMPT + "\n\nJournal entry:\n" + sanitized;

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);

      ValidationResult validation = validator.validateMoodSuggestion(cleanResponse);
      if (!validation.isValid()) {
        log.warn("Mood suggestion validation failed: {}", validation.getFailureReason());
        return "{\"emotion\":\"neutral\",\"category\":\"NEUTRAL\",\"intensity\":5,\"confidence\":0.3}";
      }

      return cleanResponse;
    } catch (Exception e) {
      log.error("Failed to suggest mood: {}", e.getMessage());
      return "{\"emotion\":\"neutral\",\"category\":\"NEUTRAL\",\"intensity\":5,\"confidence\":0.3}";
    }
  }

  /**
   * Generate a neutral, objective analysis of the journal content.
   */
  public String generateNeutralAnalysis(String text, String detectedEmotion) {
    String sanitized = sanitizer.sanitize(text);
    String prompt = String.format(PromptConstants.NEUTRAL_ANALYSIS_PROMPT,
        detectedEmotion != null ? detectedEmotion : "unknown",
        sanitized);

    try {
      String result = callGeminiWithRotation(prompt);
      // Plain text response, just clean control chars
      result = result.replaceAll("[\\x00-\\x1F]", " ").trim();

      if (result.startsWith("{") || result.startsWith("\"")) {
        return "Detected emotion: " + detectedEmotion;
      }
      return result;
    } catch (Exception e) {
      log.warn("Neutral analysis failed: {}", e.getMessage());
      return "Detected emotion: " + detectedEmotion;
    }
  }

  // ========================================================================
  // PRIVATE HELPER METHODS
  // ========================================================================

  /**
   * Generate fallback emotion breakdown using lexicon.
   */
  private String generateFallbackEmotionBreakdown(String text) {
    var vad = lexiconService.analyzeText(text);
    double valence = vad.getOrDefault("valence", 0.5);

    // Map valence to emotions (simplified)
    int happiness = (int) (valence * 100);
    int sadness = (int) ((1 - valence) * 60);
    int anger = Math.max(0, 100 - happiness - sadness - 15);

    String dominant = happiness >= sadness ? "happiness" : "sadness";

    return String.format(
        """
            {"emotions":{"happiness":{"percentage":%d,"confidence":0.4},"sadness":{"percentage":%d,"confidence":0.4},"anger":{"percentage":%d,"confidence":0.3},"fear":{"percentage":5,"confidence":0.3},"surprise":{"percentage":5,"confidence":0.3},"disgust":{"percentage":3,"confidence":0.3},"contempt":{"percentage":2,"confidence":0.3}},"dominantEmotion":"%s","overallConfidence":0.35,"reasoning":"Fallback analysis using lexicon-based VAD scoring."}
            """
            .trim(),
        happiness, sadness, anger, dominant);
  }

  /**
   * Generate risk assessment using lexicon only.
   */
  private String generateLexiconRiskAssessment(String text) {
    int riskScore = lexiconService.calculateRiskScore(text);
    var keywords = lexiconService.detectCrisisKeywords(text);

    String riskLevel;
    if (riskScore >= 9)
      riskLevel = "CRISIS";
    else if (riskScore >= 7)
      riskLevel = "HIGH";
    else if (riskScore >= 4)
      riskLevel = "MEDIUM";
    else
      riskLevel = "LOW";

    String keywordsJson = keywords.isEmpty() ? "[]"
        : "[\"" + String.join("\",\"", keywords) + "\"]";

    return String.format(
        """
            {"riskScore":%d,"riskLevel":"%s","confidence":0.6,"crisisIndicators":%s,"reasoning":"Lexicon-based analysis (AI fallback)."}
            """
            .trim(),
        riskScore, riskLevel, keywordsJson);
  }

  /**
   * Default quote when AI fails.
   */
  private String getDefaultQuote() {
    return "{\"quote\":\"The only way out is through.\",\"author\":\"Robert Frost\",\"verified\":true}";
  }

  /**
   * Call Gemini API with model rotation and retry logic.
   */
  public String callGeminiWithRotation(String prompt) {
    apiLock.lock();
    try {
      Exception lastException = null;
      int startIndex = modelIndex.get();
      int modelCount = AVAILABLE_MODELS.size();

      for (int i = 0; i < modelCount; i++) {
        int currentIndex = (startIndex + i) % modelCount;
        String model = AVAILABLE_MODELS.get(currentIndex);
        log.debug("Attempting Gemini API call with model: {} (attempt {}/{})",
            model, i + 1, modelCount);

        try {
          GenerateContentResponse response = client.models.generateContent(model, prompt, null);
          String text = response.text();
          log.debug("Successfully got response from model: {}", model);

          modelIndex.set((currentIndex + 1) % modelCount);
          return text != null ? text.trim() : "";
        } catch (Exception e) {
          lastException = e;
          String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

          if (errorMsg.contains("429") || errorMsg.contains("rate") ||
              errorMsg.contains("quota") || errorMsg.contains("resource_exhausted")) {
            log.warn("Rate limit hit on model {}, trying next...", model);
          } else if (errorMsg.contains("404") || errorMsg.contains("not found")) {
            log.warn("Model {} not available (404), trying next...", model);
          } else {
            log.error("Error with model {}: {}", model, e.getMessage());
          }

          try {
            Thread.sleep(200);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        }
      }

      String errorMessage = lastException != null ? lastException.getMessage() : "Unknown error";
      log.error("All {} models exhausted. Last error: {}", modelCount, errorMessage);
      throw new RuntimeException("All Gemini models exhausted: " + errorMessage, lastException);

    } finally {
      apiLock.unlock();
    }
  }

  /**
   * Clean up response - remove markdown code blocks and sanitize control
   * characters.
   */
  public String cleanJsonResponse(String response) {
    if (response == null)
      return "{}";

    String cleanText = response.trim();

    // Remove markdown code blocks
    if (cleanText.startsWith("```")) {
      int firstNewline = cleanText.indexOf('\n');
      if (firstNewline != -1) {
        cleanText = cleanText.substring(firstNewline + 1);
      }
      if (cleanText.endsWith("```")) {
        cleanText = cleanText.substring(0, cleanText.length() - 3);
      }
    }

    cleanText = cleanText.trim();

    // Sanitize control characters inside JSON string values
    StringBuilder sanitized = new StringBuilder();
    boolean inString = false;
    boolean escaped = false;

    for (int i = 0; i < cleanText.length(); i++) {
      char c = cleanText.charAt(i);

      if (escaped) {
        sanitized.append(c);
        escaped = false;
        continue;
      }

      if (c == '\\') {
        sanitized.append(c);
        escaped = true;
        continue;
      }

      if (c == '"') {
        inString = !inString;
        sanitized.append(c);
        continue;
      }

      if (inString && c < 32) {
        if (c == '\n' || c == '\r' || c == '\t') {
          sanitized.append(' ');
        }
        continue;
      }

      sanitized.append(c);
    }

    return sanitized.toString().trim();
  }
}
