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
        // Using "dummy-key" to avoid real API calls failing during initialization
        geminiService = new GeminiService("dummy-key", validator, lexiconService, sanitizer, restTemplate, objectMapper,
                aiSecurityService);
    }

    @Test
    void testThrottlingEnforced() {
        // We'll call a method that triggers throttle().
        // Since we can't easily mock the final 'client' field inside GeminiService
        // without PowerMock
        // we'll expect an exception when it tries to call the real API,
        // but we'll measure the time taken for the throttle() call to trigger.

        long start = System.currentTimeMillis();

        // First call - sets the lastCallTimestamp
        try {
            geminiService.getEmbedding("test 1");
        } catch (Exception ignored) {
            // Expected to fail because of dummy-key and real Client call
        }

        long firstCallEnd = System.currentTimeMillis();

        // Second call - should be throttled
        try {
            geminiService.getEmbedding("test 2");
        } catch (Exception ignored) {
        }

        long secondCallEnd = System.currentTimeMillis();
        long duration = secondCallEnd - firstCallEnd;

        System.out.println("Time between calls: " + duration + "ms");

        // Should be at least 6000ms (THROTTLE_MS)
        // Allowing a small margin if needed, but throttle() uses Thread.sleep(waitTime)
        assertTrue(duration >= 5900,
                "Throttling should enforce at least ~6 seconds between calls. Actual: " + duration + "ms");
    }
}
