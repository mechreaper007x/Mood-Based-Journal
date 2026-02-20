package com.example.moodjournal.exception;

/**
 * Thrown when the Gemini API rate limit bucket is exhausted.
 *
 * <p>
 * This signals a <strong>fail-fast</strong> rejection: the caller should
 * receive an
 * HTTP 429 immediately rather than waiting for a thread to sleep, which would
 * stall a Tomcat worker thread and starve the thread pool under load.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
