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

import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final AISecurityService aiSecurityService;

  @Value("${google.api.key}")
  private String geminiApiKey;

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
      "gemma-3-1b-it");

  // Round-robin counter for model selection
  private final AtomicInteger modelIndex = new AtomicInteger(0);

  // Lock to ensure only one API call at a time
  private final ReentrantLock apiLock = new ReentrantLock();

  // Rate limiting: 10 RPM = 1 request every 6 seconds.
  private static final long THROTTLE_MS = 6000;
  private long lastCallTimestamp = 0;

  public GeminiService(
      @Value("${google.api.key}") String rawApiKey,
      AIResponseValidator validator,
      VADLexiconService lexiconService,
      com.example.moodjournal.security.sanitization.InputSanitizer sanitizer,
      RestTemplate restTemplate, ObjectMapper objectMapper, AISecurityService aiSecurityService) {
    this.client = Client.builder().apiKey(rawApiKey).build();
    this.geminiApiKey = rawApiKey;
    this.validator = validator;
    this.lexiconService = lexiconService;
    this.sanitizer = sanitizer;
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.aiSecurityService = aiSecurityService;
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
  public String analyzeEmotions(String entryContent) {
    // SECURITY: Pre-process and filter input
    String safeContent = aiSecurityService.securePrompt(entryContent);

    // If content was completely filtered (e.g. only PII), warn but proceed with
    // empty or handle as error?
    // securePrompt throws SecurityException if jailbreak detected.

    String prompt = """
        Analyze the following journal entry and return a JSON object with:
        1. "DominantEmotion": The single most prominent emotion.
        2. "SentimentScore": A number from -1 (negative) to 1 (positive).
        3. "Keywords": A list of up to 5 key themes/topics.
        4. "Suggestion": A brief, helpful suggestion for the user.

        Entry: "%s"
        """.formatted(safeContent); // Use sanitized content

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);
      log.info("Gemini emotion analysis response (raw): {}", cleanResponse);

      // Validate and check confidence
      ValidationResult validation = validator.validateEmotionBreakdown(cleanResponse);

      if (!validation.isValid()) {
        log.warn("AI response validation failed: {}. Using fallback.", validation.getFailureReason());
        return generateFallbackEmotionBreakdown(safeContent);
      }

      if (validator.shouldFallbackToLexicon(validation.getConfidence())) {
        String enhancedResponse = lexiconService.enhanceAnalysis(entryContent, validation);
        return aiSecurityService.secureResponse(enhancedResponse); // SECURITY: Output Filter
      }

      return aiSecurityService.secureResponse(cleanResponse); // SECURITY: Output Filter
    } catch (Exception e) {
      log.error("Gemini analysis failed: {}. Using fallback.", e.getMessage(), e);
      return aiSecurityService.secureResponse(generateFallbackEmotionBreakdown(safeContent)); // SECURITY: Output Filter
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

  /**
   * Call Gemini API with model rotation and retry logic.
   */
  public String chat(String message) {
    // SECURITY: Pre-process and filter input
    String safeMessage = aiSecurityService.securePrompt(message);

    String prompt = """
        You are a helpful, empathetic mental health companion.
        User says: "%s"
        Reply in a supportive, conversational manner. Keep it brief (under 50 words).
        """.formatted(safeMessage); // Use sanitized content

    String response = callGeminiWithRotation(prompt);
    return aiSecurityService.secureResponse(response); // SECURITY: Output Filter
  }

  // ========================================================================
  // PRIVATE HELPER METHODS
  // ========================================================================

  /**
   * Generate fallback emotion breakdown using lexicon.
   */
  private String generateFallbackEmotionBreakdown(String text) {
    String fallback = lexiconService.analyzeWithoutAI(text);
    return aiSecurityService.secureResponse(fallback);
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
    throttle(); // Ensure rate limiting
    Exception lastException = null;
    int startIndex = modelIndex.get();
    int modelCount = AVAILABLE_MODELS.size();

    for (int i = 0; i < modelCount; i++) {
      int currentIndex = (startIndex + i) % modelCount;
      String model = AVAILABLE_MODELS.get(currentIndex);
      log.debug("Attempting Gemini API call with model: {} (attempt {}/{})",
          model, i + 1, modelCount);

      // Response scoped to try block - eligible for GC immediately after return
      // NOTE: Setting response = null in finally block was ineffective
      // because the local variable goes out of scope anyway
      try {
        GenerateContentResponse response = client.models.generateContent(model, prompt, null);
        String text = response.text();
        log.debug("Successfully got response from model: {}", model);

        // Atomic rotation (V7 fix)
        modelIndex.getAndUpdate(current -> (current + 1) % modelCount);
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
      }
      // No finally block - response variable is scoped to try block

      // Backoff (outside try-catch to allow interrupt)
      if (i < modelCount - 1) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    String errorMessage = lastException != null ? lastException.getMessage() : "Unknown error";
    log.error("All {} models exhausted. Last error: {}", modelCount, errorMessage);
    throw new RuntimeException("All Gemini models exhausted: " + errorMessage, lastException);
  }

  // ========================================================================
  // EMBEDDING SUPPORT (Vector RAG)
  // ========================================================================

  /**
   * Generates a vector embedding for the given text using 'text-embedding-004'.
   * Returns a float array representing the semantic meaning.
   */
  public float[] getEmbedding(String text) {
    throttle(); // Ensure rate limiting
    try {
      // Model: gemini-embedding-001 is the embedding model for v1beta API
      String modelName = "gemini-embedding-001";

      var response = client.models.embedContent(modelName, text, null);
      // API: response.embeddings() returns Optional<List<ContentEmbedding>>
      // Each ContentEmbedding has values() returning Optional<List<Float>>
      List<Float> values = response.embeddings().get().get(0).values().get();

      // Convert List<Float> to float[]
      float[] result = new float[values.size()];
      for (int i = 0; i < values.size(); i++) {
        result[i] = values.get(i);
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to generate embedding: {}", e.getMessage());
      // Return empty array or throw? For now throw to handle retry in caller.
      throw new RuntimeException("Embedding generation failed", e);
    }
  }

  /**
   * Ensures that subsequent API calls are spaced out by at least THROTTLE_MS.
   */
  private void throttle() {
    apiLock.lock();
    try {
      long currentTime = System.currentTimeMillis();
      long timeSinceLastCall = currentTime - lastCallTimestamp;

      if (timeSinceLastCall < THROTTLE_MS) {
        long waitTime = THROTTLE_MS - timeSinceLastCall;
        log.info("Throttling Gemini API call ({} RPM limit). Waiting {}ms...", (60000 / THROTTLE_MS), waitTime);
        try {
          Thread.sleep(waitTime);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Throttling interrupted: {}", e.getMessage());
        }
      }
      lastCallTimestamp = System.currentTimeMillis();
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
