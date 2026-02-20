package com.example.moodjournal.security.sanitization;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;







@Component
public class InputSanitizer {

    
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore previous instructions|system:|assistant:|user:|developed by|initial prompt)",
            Pattern.CASE_INSENSITIVE);

    
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]++@[a-zA-Z0-9.-]++\\.[a-zA-Z]{2,6}+");

    


    public String sanitize(String input) {
        if (input == null) {
            return "";
        }

        
        String safe = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        
        safe = safe.replaceAll("```[^`]*```", "[CODE_BLOCK_REMOVED]")
                .replaceAll("~~~[^~]*~~~", "[CODE_BLOCK_REMOVED]");

        
        safe = INJECTION_PATTERN.matcher(safe).replaceAll("[Redacted Command]");

        
        
        if (safe.length() > 5000) {
            safe = safe.substring(0, 5000); 
        }

        
        safe = EMAIL_PATTERN.matcher(safe).replaceAll("[EMAIL]");

        
        safe = safe.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        return safe;
    }
}
