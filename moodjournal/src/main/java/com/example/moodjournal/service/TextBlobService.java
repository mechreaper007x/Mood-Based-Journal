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

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TextBlobService {

    private static final Logger log = LoggerFactory.getLogger(TextBlobService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${textblob.service.url:http://localhost:5001/analyze}")
    private String serviceUrl;

    public String analyzeMood(String text) {
        log.info("Calling TextBlob service for text length: {}", text.length());

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

            log.info("TextBlob service response: {}", response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("Error calling TextBlob service", e);
            // Return a fallback response that the parser can handle - sets mood to NEUTRAL
            return "{\"emotion\": \"neutral\", \"category\": \"NEUTRAL\", \"intensity\": 5.0}";
        }
    }
}
