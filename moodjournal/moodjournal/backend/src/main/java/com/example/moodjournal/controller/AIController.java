package com.example.moodjournal.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.dto.SuggestMoodRequest;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.service.GeminiService;
import com.example.moodjournal.service.JournalEntryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@CrossOrigin
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final GeminiService geminiService;
    private final JournalEntryService journalEntryService;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    public AIController(GeminiService geminiService, JournalEntryService journalEntryService,
            ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.journalEntryService = journalEntryService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/suggest-mood", produces = "application/json")
    public ResponseEntity<?> suggestMood(@RequestBody SuggestMoodRequest request) {
        Map<String, String> validationError = validateTextRequest(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(validationError);
        }
        String content = request.getContent();
        Mood suggestedMood = journalEntryService.suggestMood(content);
        return ResponseEntity.ok(Map.of("suggestedMood", suggestedMood.name()));
    }

    @PostMapping(value = "/emotion-breakdown", produces = "application/json")
    public java.util.concurrent.CompletableFuture<ResponseEntity<?>> emotionBreakdown(
            @RequestBody SuggestMoodRequest request) {
        Map<String, String> validationError = validateTextRequest(request);
        if (validationError != null) {
            return java.util.concurrent.CompletableFuture
                    .completedFuture(ResponseEntity.badRequest().body(validationError));
        }
        String content = request.getContent();

        return geminiService.getEmotionBreakdown(content)
                .<ResponseEntity<?>>thenApply(response -> {
                    try {
                        log.info("Received emotion analysis response: {}", response);
                        Map<String, Object> json = objectMapper.readValue(response,
                                new TypeReference<Map<String, Object>>() {
                                });
                        return ResponseEntity.ok((Object) json);
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing AI response: {}", response, e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Failed to parse AI response: " + e.getMessage()));
                    }
                })
                .exceptionally(e -> {
                    log.error("Error calling Gemini API", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "AI analysis failed: " + e.getMessage()));
                });
    }

    private Map<String, String> validateTextRequest(SuggestMoodRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            return Map.of("error", "Text cannot be empty");
        }
        return null; // Validation passed
    }

    @GetMapping(value = "/daily-quote", produces = "application/json")
    public ResponseEntity<Object> dailyQuote() {
        try {
            String response = geminiService.getDailyQuote();
            log.info("Daily quote raw response: {}", response);
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = objectMapper.readValue(response, Map.class);
                // Check if the response contains an error
                if (json.containsKey("error")) {
                    log.warn("AI service returned error: {}", json.get("error"));
                    // Return a fallback quote
                    return ResponseEntity.ok(Map.of(
                            "quote", "You are stronger than you know. Take it one breath at a time.",
                            "author", "Unknown"));
                }
                return ResponseEntity.ok(json);
            } catch (JsonProcessingException e) {
                log.error("Error parsing AI response: {}", response, e);
                return ResponseEntity.ok(Map.of(
                        "quote", "Healing comes in waves. It's okay to not be okay today.",
                        "author", "Unknown"));
            }
        } catch (Exception e) {
            log.error("Unexpected error in dailyQuote", e);
            return ResponseEntity.ok(Map.of(
                    "quote", "Your feelings are valid, and this moment will pass.",
                    "author", "Unknown"));
        }
    }
}