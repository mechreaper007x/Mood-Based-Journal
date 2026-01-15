package com.example.moodjournal.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.moodjournal.util.PromptConstants;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

/**
 * Service for AI-powered text generation using Google Gemini SDK.
 * Uses round-robin model rotation to distribute load across available models.
 * Synchronized API calls prevent concurrent requests from hitting rate limits.
 */
@Service
public class GeminiService {

  private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

  private final Client client;

  // Available models for rotation (Gemini models first, then Gemma
  // instruction-tuned variants)
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

  public GeminiService(@Value("${google.api.key}") String apiKey) {
    this.client = Client.builder().apiKey(apiKey).build();
    log.info("GeminiService initialized with {} models for rotation: {}",
        AVAILABLE_MODELS.size(), AVAILABLE_MODELS);
  }

  @org.springframework.scheduling.annotation.Async("taskExecutor")
  public java.util.concurrent.CompletableFuture<String> getEmotionBreakdown(String text) {
    String prompt = PromptConstants.EMOTION_BREAKDOWN_PROMPT + "\n\nAnalyze this entry: " + text;
    String result = callGeminiWithRotation(prompt);
    return java.util.concurrent.CompletableFuture.completedFuture(result);
  }

  public String getDailyQuote() {
    try {
      String prompt = PromptConstants.DAILY_QUOTE_PROMPT + "\n\nGive me a quote of the day.";
      return callGeminiWithRotation(prompt);
    } catch (Exception e) {
      log.error("Failed to get daily quote: {}", e.getMessage());
      return "{\"quote\": \"Stay positive and keep moving forward.\", \"author\": \"Unknown\"}";
    }
  }

  /**
   * Analyze emotions in journal content and return percentage breakdown.
   * Returns strict JSON with emotion percentages and dominant emotion.
   */
  public String analyzeEmotions(String journalContent) {
    String prompt = """
        Analyze the following text for emotional sentiment. Output a strict JSON object with estimated
        percentage values for 'Anger', 'Happy', and 'Sadness', and identify the 'DominantEmotion'.
        Ensure the percentages reflect the intensity and frequency of emotional cues in the text.

        The percentages should be strings with '%' suffix (e.g. "60%").

        Reply in STRICT JSON format only. No markdown, no explanation, just pure JSON:
        {
          "Anger": "<0-100>%",
          "Happy": "<0-100>%",
          "Sadness": "<0-100>%",
          "DominantEmotion": "<Anger|Happy|Sadness>"
        }

        Rules:
        - Percentages must add up to 100%
        - DominantEmotion must be the emotion with highest percentage
        - If text is neutral, distribute evenly but still pick a dominant

        Text to analyze:
        """ + journalContent;

    try {
      String response = callGeminiWithRotation(prompt);
      log.info("Gemini emotion analysis response: {}", response);
      return cleanJsonResponse(response);
    } catch (Exception e) {
      log.error("Gemini Analysis Failed: {}", e.getMessage(), e);
      return "{\"error\": \"Analysis failed: " + e.getMessage() + "\"}";
    }
  }

  public String suggestMood(String text) {
    try {
      String prompt = PromptConstants.SUGGEST_MOOD_PROMPT + "\n\nSuggest a mood for this entry: " + text;
      return callGeminiWithRotation(prompt);
    } catch (Exception e) {
      log.error("Failed to suggest mood: {}", e.getMessage());
      return "NEUTRAL";
    }
  }

  /**
   * Generate a neutral, objective analysis of the journal content.
   */
  public String generateNeutralAnalysis(String text, String detectedEmotion) {
    String prompt = """
        You are a neutral, objective analyst. Provide a brief 2-3 sentence analysis of the emotional content.
        Be factual and observational. Do not give advice or be clinical.
        Focus on: what emotions are expressed, the general tone, and any notable themes.
        The NLP system detected the primary emotion as: %s
        Return only plain text, no JSON, no markdown.

        Text to analyze:
        %s
        """.formatted(detectedEmotion != null ? detectedEmotion : "unknown", text);

    try {
      String result = callGeminiWithRotation(prompt);
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
   * Uses a lock to ensure only one API call happens at a time.
   * Tries ALL models starting from the current rotation index.
   */
  public String callGeminiWithRotation(String prompt) {
    apiLock.lock();
    try {
      Exception lastException = null;
      int startIndex = modelIndex.get(); // Get current position, don't increment yet
      int modelCount = AVAILABLE_MODELS.size();

      // Try each model starting from current position
      for (int i = 0; i < modelCount; i++) {
        int currentIndex = (startIndex + i) % modelCount;
        String model = AVAILABLE_MODELS.get(currentIndex);
        log.info("Attempting Gemini API call with model: {} (attempt {}/{}, index {})",
            model, i + 1, modelCount, currentIndex);

        try {
          GenerateContentResponse response = client.models.generateContent(model, prompt, null);
          String text = response.text();
          log.info("Successfully got response from model: {}", model);

          // Only increment global counter on SUCCESS - next request starts with next
          // model
          modelIndex.set((currentIndex + 1) % modelCount);

          return text != null ? text.trim() : "";
        } catch (Exception e) {
          lastException = e;
          String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

          // Check if it's a rate limit error (429)
          if (errorMsg.contains("429") || errorMsg.contains("rate") ||
              errorMsg.contains("quota") || errorMsg.contains("resource_exhausted")) {
            log.warn("Rate limit hit on model {}, trying next model...", model);
          } else if (errorMsg.contains("404") || errorMsg.contains("not found")) {
            log.warn("Model {} not available (404), trying next model...", model);
          } else {
            log.error("Error with model {}: {}", model, e.getMessage());
          }

          // Small delay before trying next model
          try {
            Thread.sleep(200);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        }
      }

      // All models exhausted
      String errorMessage = lastException != null ? lastException.getMessage() : "Unknown error";
      log.error("All {} models exhausted. Last error: {}", modelCount, errorMessage);
      throw new RuntimeException("All Gemini models exhausted: " + errorMessage, lastException);

    } finally {
      apiLock.unlock();
    }
  }

  /**
   * Clean up response - remove markdown code blocks and sanitize control
   * characters
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
    // Replace literal newlines/tabs with escaped versions or spaces
    // This handles Gemini returning unescaped control chars in JSON
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

      // If inside a string, replace control characters
      if (inString && c < 32) {
        // Replace newline/carriage return/tab with space
        if (c == '\n' || c == '\r' || c == '\t') {
          sanitized.append(' ');
        }
        // Skip other control characters
        continue;
      }

      sanitized.append(c);
    }

    return sanitized.toString().trim();
  }
}
