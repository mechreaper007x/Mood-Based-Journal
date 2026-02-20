package com.example.moodjournal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.moodjournal.security.sanitization.InputSanitizer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ThrottlingTest {

    private GeminiService geminiService;

    @Mock
    private AIResponseValidator validator;
    @Mock
    private VADLexiconService lexiconService;
    @Mock
    private InputSanitizer sanitizer;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AISecurityService aiSecurityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        geminiService = new GeminiService("dummy-key", validator, lexiconService, sanitizer, restTemplate, objectMapper,
                aiSecurityService);
    }

    @Test
    void testThrottlingEnforced() {
        
        
        
        
        

        long start = System.currentTimeMillis();

        
        try {
            geminiService.getEmbedding("test 1");
        } catch (Exception ignored) {
            
        }

        long firstCallEnd = System.currentTimeMillis();

        
        try {
            geminiService.getEmbedding("test 2");
        } catch (Exception ignored) {
        }

        long secondCallEnd = System.currentTimeMillis();
        long duration = secondCallEnd - firstCallEnd;

        System.out.println("Time between calls: " + duration + "ms");

        
        
        assertTrue(duration >= 5900,
                "Throttling should enforce at least ~6 seconds between calls. Actual: " + duration + "ms");
    }
}
