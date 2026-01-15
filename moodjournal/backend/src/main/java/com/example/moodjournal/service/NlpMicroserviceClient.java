package com.example.moodjournal.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.moodjournal.dto.NlpAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Client for the Python NLP microservice (spaCy + TextBlob + VADER)
 * Implements Steps 2-4 of the 7-step NLP pipeline
 */
@Service
public class NlpMicroserviceClient {

    private static final Logger log = LoggerFactory.getLogger(NlpMicroserviceClient.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nlp.service.url:http://localhost:5001/analyze}")
    private String serviceUrl;

    /**
     * Analyze text using the Python NLP microservice
     * 
     * @param text The journal content to analyze
     * @return Structured analysis result with polarity, sentiment, and emotion
     */
    public NlpAnalysisResult analyze(String text) {
        log.info("Calling NLP microservice for text length: {}", text.length());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBodyMap = Map.of("text", text);

        try {
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    serviceUrl,
                    entity,
                    String.class);

            log.info("NLP service response received");
            return objectMapper.readValue(response.getBody(), NlpAnalysisResult.class);

        } catch (Exception e) {
            log.error("Error calling NLP microservice: {}", e.getMessage());
            // Return fallback result
            return createFallbackResult();
        }
    }

    /**
     * Legacy method for backward compatibility with existing code
     */
    public String analyzeMood(String text) {
        NlpAnalysisResult result = analyze(text);
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "emotion", result.getEmotion(),
                    "category", result.getCategory(),
                    "intensity", Math.abs(result.getVaderCompound() != null ? result.getVaderCompound() * 10 : 5.0)));
        } catch (Exception e) {
            return "{\"emotion\": \"neutral\", \"category\": \"NEUTRAL\", \"intensity\": 5.0}";
        }
    }

    private NlpAnalysisResult createFallbackResult() {
        NlpAnalysisResult fallback = new NlpAnalysisResult();
        fallback.setTextblobPolarity(0.0);
        fallback.setTextblobSubjectivity(0.5);
        fallback.setVaderCompound(0.0);
        fallback.setVaderPositive(0.0);
        fallback.setVaderNegative(0.0);
        fallback.setVaderNeutral(1.0);
        fallback.setBaseSentiment("NEUTRAL");
        fallback.setEmotion("neutral");
        fallback.setCategory("NEUTRAL");
        return fallback;
    }
}
