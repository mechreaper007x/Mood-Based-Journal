package com.example.moodjournal.security.sanitization;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Utility for sanitizing user inputs to prevent:
 * 1. Prompt Injection attacks
 * 2. PII leakage (basic)
 * 3. XSS vectors (though output is usually JSON)
 */
@Component
public class InputSanitizer {

    // Patterns that attempt to override system instructions
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore previous instructions|system:|assistant:|user:|developed by|initial prompt)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Sanitizes input text for AI consumption.
     */
    public String sanitize(String input) {
        if (input == null) {
            return "";
        }

        String safe = input;

        // 1. Strip potential prompt injection delimiters
        safe = INJECTION_PATTERN.matcher(safe).replaceAll("[Redacted Command]");

        // 2. Limit length to prevent context flooding (e.g., 5000 chars)
        if (safe.length() > 5000) {
            safe = safe.substring(0, 5000) + "... [Truncated]";
        }

        // 3. Basic PII redaction (Example: Email addresses)
        // Note: For a real app, use a dedicated library like Google DLC or Microsoft
        // Presidio
        safe = safe.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}", "[EMAIL_REDACTED]");

        return safe;
    }
}
