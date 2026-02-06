package com.example.moodjournal.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class SanitizationService {

    /**
     * Sanitizes the input string by stripping out unsafe HTML tags and attributes.
     * Uses Jsoup's Safelist.basic() which allows simple text formatting (b, i, u)
     * but removes scripts, styles, and event handlers.
     *
     * @param input The untrusted input string.
     * @return The sanitized string.
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // Safelist.basic() allows: a, b, blockquote, br, cite, code, dd, dl, dt, em, i,
        // li, ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul
        // It actively strips <script>, <object>, <iframe>, and event handlers (onclick,
        // etc.)
        return Jsoup.clean(input, Safelist.basic());
    }

    /**
     * Stricter sanitization that removes ALL HTML tags, leaving only text.
     * Use this for fields that should never contain markup (e.g., titles).
     */
    public String sanitizeStrict(String input) {
        if (input == null) {
            return null;
        }
        return Jsoup.clean(input, Safelist.none());
    }
}
