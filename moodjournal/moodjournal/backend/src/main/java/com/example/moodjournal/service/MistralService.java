package com.example.moodjournal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.moodjournal.model.JournalEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for generating personalized assessment questions using Mistral AI.
 * Analyzes user's journal entries to create tailored psychological questions.
 */
@Service
public class MistralService {

    private static final Logger log = LoggerFactory.getLogger(MistralService.class);

    @Value("${mistral.api.key:}")
    private String apiKey;

    @Value("${mistral.api.url:https://api.mistral.ai/v1/chat/completions}")
    private String apiUrl;

    @Value("${mistral.model:mistral-small-latest}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate 3 personalized psychological questions based on journal entries.
     * Questions focus on: Big 5 traits, Shadow/Enneagram insights, Personal growth.
     */
    public List<Map<String, Object>> generatePersonalizedQuestions(List<JournalEntry> recentEntries) {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🎯 generatePersonalizedQuestions called");
        log.info("   API Key configured: {}", apiKey != null && !apiKey.isBlank());
        log.info("   Journal entries received: {}", recentEntries.size());

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("❌ Mistral API key not configured, returning fallback questions");
            return getFallbackQuestions();
        }

        if (recentEntries.isEmpty()) {
            log.info("⚠️ No journal entries available - returning fallback questions");
            log.info("   TIP: User needs to write at least 1 journal entry first!");
            return getFallbackQuestions();
        }

        // Build journal context
        StringBuilder journalContext = new StringBuilder();
        for (int i = 0; i < Math.min(5, recentEntries.size()); i++) {
            JournalEntry entry = recentEntries.get(i);
            journalContext.append("Entry ").append(i + 1).append(": ")
                    .append(entry.getContent().substring(0, Math.min(300, entry.getContent().length())))
                    .append("...\n");
        }

        String prompt = """
                You are a clinical psychologist conducting a personalized assessment.
                Based on these recent journal entries, generate 3 tailored questions:

                --- JOURNAL ENTRIES ---
                %s
                --- END ENTRIES ---

                Generate exactly 3 questions:
                1. One that explores personality patterns (Big 5 related)
                2. One that explores core motivations/fears (Enneagram/Shadow related)
                3. One that addresses a specific personal theme from the entries

                Each question should:
                - Be open-ended and reflective
                - Reference specific themes from the entries without quoting directly
                - Be non-judgmental and safe to answer

                Return ONLY a JSON array, no markdown:
                [
                  {"id": 1, "question": "...", "focus": "personality"},
                  {"id": 2, "question": "...", "focus": "shadow"},
                  {"id": 3, "question": "...", "focus": "personal"}
                ]
                """.formatted(journalContext.toString());

        try {
            log.info("═══════════════════════════════════════════════════════════");
            log.info("🤖 MISTRAL API CALL - Generating Personalized Questions");
            log.info("═══════════════════════════════════════════════════════════");
            log.info("📚 Journal entries used: {}", Math.min(5, recentEntries.size()));
            log.info("📝 Context preview: {}...", journalContext.substring(0, Math.min(100, journalContext.length())));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = Map.of("role", "user", "content", prompt);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(message),
                    "temperature", 0.7,
                    "max_tokens", 500);

            log.info("🔗 Calling Mistral API: {} | Model: {}", apiUrl, model);
            long startTime = System.currentTimeMillis();

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(apiUrl, request, String.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("⏱️  API Response Time: {}ms", duration);

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Clean and parse JSON
            String cleanJson = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            List<Map<String, Object>> questions = objectMapper.readValue(
                    cleanJson, new TypeReference<List<Map<String, Object>>>() {
                    });

            log.info("✅ Generated {} personalized questions:", questions.size());
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                log.info("   Q{}: [{}] {}", i + 1, q.get("focus"), q.get("question"));
            }
            log.info("═══════════════════════════════════════════════════════════");

            return questions;

        } catch (Exception e) {
            log.error("❌ Mistral API call failed: {}", e.getMessage(), e);
            log.info("⚠️  Returning fallback questions");
            return getFallbackQuestions();
        }
    }

    private List<Map<String, Object>> getFallbackQuestions() {
        List<Map<String, Object>> questions = new ArrayList<>();
        questions.add(Map.of(
                "id", 1,
                "question", "What patterns do you notice in your emotional responses lately?",
                "focus", "personality"));
        questions.add(Map.of(
                "id", 2,
                "question", "What fears or anxieties seem to drive your behavior most often?",
                "focus", "shadow"));
        questions.add(Map.of(
                "id", 3,
                "question", "What brings you the most fulfillment in your daily life?",
                "focus", "personal"));
        return questions;
    }
}
