package com.example.moodjournal.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.moodjournal.exception.RateLimitExceededException;
import com.example.moodjournal.service.AIResponseValidator.ValidationResult;
import com.example.moodjournal.util.PromptConstants;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Service
public class GeminiService {

  private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

  private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final AISecurityService aiSecurityService;

  @Value("${google.api.key}")
  private String geminiApiKey;

  @Value("${HF_TOKEN:}")
  private String hfToken;


  private final Client client;
  private final AIResponseValidator validator;
  private final VADLexiconService lexiconService;
  private final com.example.moodjournal.security.sanitization.InputSanitizer sanitizer;

  private static final List<String> AVAILABLE_MODELS = List.of(
      "gemini-2.5-flash",
      "gemini-2.5-flash-lite",
      "gemini-3-flash",
      "gemma-3-27b-it",
      "gemma-3-12b-it",
      "gemma-3-4b-it",
      "gemma-3-1b-it");

  private final AtomicInteger modelIndex = new AtomicInteger(0);

  /**
   * Token bucket: 15 tokens refilled every 60 seconds (Gemini free-tier RPM
   * limit).
   * Bucket4j is thread-safe by design; no external lock is needed.
   */
  private final Bucket rateLimiter = Bucket.builder()
      .addLimit(Bandwidth.builder()
          .capacity(15)
          .refillGreedy(15, Duration.ofMinutes(1))
          .build())
      .build();

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
    log.info("GeminiService initialized. Models: {}, Rate limit: 15 req/min (Bucket4j)",
        AVAILABLE_MODELS.size());
  }

  @Async("taskExecutor")
  public CompletableFuture<String> getEmotionBreakdown(String text) {
    String safeText = secureAndSanitizeInput(text);
    String prompt = PromptConstants.EMOTION_BREAKDOWN_PROMPT + safeText;

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);

      ValidationResult validation = validator.validateEmotionBreakdown(cleanResponse);
      if (!validation.isValid() || validator.shouldFallbackToLexicon(validation.getConfidence())) {
        log.warn("Emotion breakdown validation failed or low confidence: {}", validation);
        return CompletableFuture.completedFuture(generateFallbackEmotionBreakdown(safeText));
      }

      return CompletableFuture.completedFuture(cleanResponse);
    } catch (Exception e) {
      log.error("Emotion breakdown failed, using fallback: {}", e.getMessage());
      return CompletableFuture.completedFuture(generateFallbackEmotionBreakdown(safeText));
    }
  }

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

  public String analyzeEmotions(String entryContent) {
    String safeContent = secureAndSanitizeInput(entryContent);

    String prompt = """
        Analyze the following journal entry and return a JSON object with:
        1. "DominantEmotion": The single most prominent emotion.
        2. "SentimentScore": A number from -1 (negative) to 1 (positive).
        3. "Keywords": A list of up to 5 key themes/topics.
        4. "Suggestion": A brief, helpful suggestion for the user.

        Entry: "%s"
        """.formatted(safeContent);

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);
      log.info("Gemini emotion analysis response (raw): {}", cleanResponse);

      ValidationResult validation = validator.validateEmotionBreakdown(cleanResponse);

      if (!validation.isValid()) {
        log.warn("AI response validation failed: {}. Using fallback.", validation.getFailureReason());
        return generateFallbackEmotionBreakdown(safeContent);
      }

      if (validator.shouldFallbackToLexicon(validation.getConfidence())) {
        String enhancedResponse = lexiconService.enhanceAnalysis(safeContent, validation);
        return aiSecurityService.secureResponse(enhancedResponse);
      }

      return aiSecurityService.secureResponse(cleanResponse);
    } catch (Exception e) {
      log.error("Gemini analysis failed: {}. Using fallback.", e.getMessage(), e);
      return aiSecurityService.secureResponse(generateFallbackEmotionBreakdown(safeContent));
    }
  }

  public String assessRisk(String journalContent) {
    String safeContent = secureAndSanitizeInput(journalContent);
    String prompt = PromptConstants.RISK_ASSESSMENT_PROMPT + safeContent;

    try {
      String response = callGeminiWithRotation(prompt);
      String cleanResponse = cleanJsonResponse(response);

      ValidationResult validation = validator.validateRiskAssessment(cleanResponse);

      if (!validation.isValid()) {
        log.warn("Risk assessment validation failed: {}. Using lexicon.", validation.getFailureReason());
        return generateLexiconRiskAssessment(safeContent);
      }

      return cleanResponse;
    } catch (Exception e) {
      log.error("Risk assessment failed: {}. Using lexicon fallback.", e.getMessage());
      return generateLexiconRiskAssessment(safeContent);
    }
  }

  public String suggestMood(String text) {
    String safeText = secureAndSanitizeInput(text);
    String prompt = PromptConstants.SUGGEST_MOOD_PROMPT + "\n\nJournal entry:\n" + safeText;

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

  public String generateNeutralAnalysis(String text, String detectedEmotion) {
    String safeText = secureAndSanitizeInput(text);
    String prompt = String.format(PromptConstants.NEUTRAL_ANALYSIS_PROMPT,
        detectedEmotion != null ? detectedEmotion : "unknown",
        safeText);

    try {
      String result = callGeminiWithRotation(prompt);

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

  public String chat(String message) {
    String safeMessage = secureAndSanitizeInput(message);

    String prompt = """
        You are a helpful, empathetic mental health companion.
        User says: "%s"
        Reply in a supportive, conversational manner. Keep it brief (under 50 words).
        """.formatted(safeMessage);

    String response = callGeminiWithRotation(prompt);
    return aiSecurityService.secureResponse(response);
  }

  private String secureAndSanitizeInput(String rawInput) {
    String secured = aiSecurityService.securePrompt(rawInput);
    return sanitizer.sanitize(secured);
  }

  private String generateFallbackEmotionBreakdown(String text) {
    String fallback = lexiconService.analyzeWithoutAI(text);
    return aiSecurityService.secureResponse(fallback);
  }

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

  private String getDefaultQuote() {
    return "{\"quote\":\"The only way out is through.\",\"author\":\"Robert Frost\",\"verified\":true}";
  }

  public String callGeminiWithRotation(String prompt) {
    consumeToken(); // Fail-fast rate limit check — no sleeping, no blocking.
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
    }

    String errorMessage = lastException != null ? lastException.getMessage() : "Unknown error";
    log.error("All {} models exhausted. Last error: {}", modelCount, errorMessage);

    // Fallback to Hugging Face free Serverless Inference API
    try {
      log.info("Attempting fallback to Hugging Face Serverless Inference API...");
      return callHuggingFaceAPI(prompt);
    } catch (Exception hfEx) {
      log.error("Hugging Face fallback also failed: {}", hfEx.getMessage());
    }

    throw new RuntimeException("All Gemini models exhausted: " + errorMessage, lastException);
  }

  private String callHuggingFaceAPI(String prompt) {
    if (hfToken == null || hfToken.isBlank()) {
      log.warn("HF_TOKEN is not configured. Cannot call Hugging Face Inference API.");
      throw new RuntimeException("HF_TOKEN is blank");
    }

    // List of models to try in rotation (non-gated, free models on HF router)
    List<String> hfModels = List.of(
        "Qwen/Qwen2.5-72B-Instruct:fastest",
        "Qwen/Qwen2.5-14B-Instruct:fastest",
        "mistralai/Mistral-7B-Instruct-v0.3:fastest"
    );

    String url = "https://router.huggingface.co/v1/chat/completions";

    for (String model : hfModels) {
      log.info("Attempting Hugging Face Serverless API call with model: {}", model);
      try {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(hfToken);

        java.util.Map<String, Object> message = java.util.Map.of("role", "user", "content", prompt);
        java.util.Map<String, Object> body = java.util.Map.of(
            "model", model,
            "messages", List.of(message),
            "temperature", 0.7,
            "max_tokens", 800
        );

        org.springframework.http.HttpEntity<java.util.Map<String, Object>> request = 
            new org.springframework.http.HttpEntity<>(body, headers);

        String response = restTemplate.postForObject(url, request, String.class);
        if (response != null) {
          com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
          String content = root.path("choices").get(0).path("message").path("content").asText();
          if (content != null && !content.isBlank()) {
            log.info("Successfully got response from Hugging Face model: {}", model);
            return content.trim();
          }
        }
      } catch (Exception e) {
        log.warn("Error calling Hugging Face model {}: {}", model, e.getMessage());
      }
    }
    throw new RuntimeException("All Hugging Face models exhausted");
  }

  public float[] getEmbedding(String text) {
    consumeToken(); // Fail-fast rate limit check.
    try {

      String modelName = "gemini-embedding-001";

      var response = client.models.embedContent(modelName, text, null);

      List<Float> values = response.embeddings().get().get(0).values().get();

      float[] result = new float[values.size()];
      for (int i = 0; i < values.size(); i++) {
        result[i] = values.get(i);
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to generate embedding: {}", e.getMessage());

      throw new RuntimeException("Embedding generation failed", e);
    }
  }

  /**
   * Attempts to consume one token from the Bucket4j rate limiter.
   *
   * <p>
   * <strong>Why this is superior to ReentrantLock + Thread.sleep():</strong><br>
   * Under the M/M/1 queueing model, server utilisation ρ = λ/μ. When
   * Thread.sleep() holds a Tomcat worker thread, that thread is blocked but
   * still counted against the thread-pool capacity. As arrival rate λ rises,
   * ρ → 1 and, by Little's Law (L = λW), mean queue length L grows without
   * bound — i.e. the server collapses. A fail-fast rejection (tryConsume)
   * immediately returns capacity to the pool: ρ stays low, latency stays
   * predictable, and the system degrades gracefully instead of starving.
   *
   * <p>
   * Bucket4j uses a lock-free CAS loop internally, so it is also more
   * CPU-efficient than a ReentrantLock under concurrent access.
   *
   * @throws RateLimitExceededException if the 15 req/min budget is exhausted.
   */
  private void consumeToken() {
    if (!rateLimiter.tryConsume(1)) {
      log.warn("Gemini rate limit exhausted (15 req/min). Rejecting request fast.");
      throw new RateLimitExceededException(
          "Gemini API rate limit exceeded (15 requests/minute). Please try again shortly.");
    }
  }

  public String cleanJsonResponse(String response) {
    if (response == null)
      return "{}";

    String cleanText = response.trim();

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
