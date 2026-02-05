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

    // Email pattern using possessive quantifiers (++) to prevent ReDoS
    // Possessive quantifiers don't backtrack, preventing catastrophic backtracking
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]++@[a-zA-Z0-9.-]++\\.[a-zA-Z]{2,6}+");

    /**
     * Sanitizes input text for AI consumption.
     */
    public String sanitize(String input) {
        if (input == null) {
            return "";
        }

        // V9 FIX: Unicode normalization FIRST (NFKC)
        String safe = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // V9 FIX: Remove code block delimiters to prevent context escape
        safe = safe.replaceAll("```[^`]*```", "[CODE_BLOCK_REMOVED]")
                .replaceAll("~~~[^~]*~~~", "[CODE_BLOCK_REMOVED]");

        // 1. Strip potential prompt injection delimiters
        safe = INJECTION_PATTERN.matcher(safe).replaceAll("[Redacted Command]");

        // V9 FIX: Length limit BEFORE detailed processing to prevent resource
        // exhaustion
        if (safe.length() > 5000) {
            safe = safe.substring(0, 5000); // Do NOT append hints if truncated to avoid info leak
        }

        // 3. Basic PII redaction
        safe = EMAIL_PATTERN.matcher(safe).replaceAll("[EMAIL]");

        // V9 FIX: Strip control characters
        safe = safe.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        return safe;
    }
}
