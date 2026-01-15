package com.example.moodjournal.service;

import com.example.moodjournal.model.Mood;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class SentimentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${twinword.api.url}")
    private String twinwordApiUrl;

    @Value("${twinword.api.key}")
    private String twinwordApiKey;

    public SentimentAnalysisService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Mood analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Received empty text for sentiment analysis. Returning NEUTRAL.");
            return Mood.NEUTRAL;
        }

        String apiUrl = twinwordApiUrl + "/sentiment/analyze/"; // Assuming this is the correct endpoint

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("X-RapidAPI-Key", twinwordApiKey); // Twinword often uses RapidAPI
        headers.set("X-RapidAPI-Host", "twinword-sentiment-analysis.p.rapidapi.com"); // Example host

        // Twinword API typically expects form-urlencoded data
        String requestBody = "text=" + text;

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(apiUrl, entity, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode sentimentNode = response.getBody().get("type"); // Assuming 'type' field contains sentiment
                if (sentimentNode != null && sentimentNode.isTextual()) {
                    String sentiment = sentimentNode.asText().toUpperCase();
                    switch (sentiment) {
                        case "POSITIVE":
                            return Mood.HAPPY;
                        case "NEGATIVE":
                            return Mood.SAD;
                        case "NEUTRAL":
                            return Mood.NEUTRAL;
                        default:
                            log.warn("Unknown sentiment type from Twinword API: {}. Returning NEUTRAL.", sentiment);
                            return Mood.NEUTRAL;
                    }
                }
            }
            log.warn("Twinword API returned an unexpected response. Falling back to NEUTRAL.");
            return Mood.NEUTRAL;
        } catch (Exception e) {
            log.error("Error calling Twinword API for sentiment analysis. Falling back to NEUTRAL.", e);
            return Mood.NEUTRAL;
        }
    }

    public double getSentimentConfidence(String content, Mood mood) {
        return 0.0; // Placeholder implementation
    }
}