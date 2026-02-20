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

  
  private static final List<String> AVAILABLE_MODELS = List.of(
      "gemini-2.5-flash",
      "gemini-2.5-flash-lite",
      "gemini-3-flash",
      "gemma-3-27b-it",
      "gemma-3-12b-it",
      "gemma-3-4b-it",
      "gemma-3-1b-it");

  
  private final AtomicInteger modelIndex = new AtomicInteger(0);

  
  private final ReentrantLock apiLock = new ReentrantLock();

  
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
    throttle(); 
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

  
  
  

  



  public float[] getEmbedding(String text) {
    throttle(); 
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
