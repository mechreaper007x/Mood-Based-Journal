package com.example.moodjournal.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * User's responses to assessment questions.
 * Supports both legacy Q&A format and new structured assessments.
 */
@Data
public class AssessmentSubmission {
    // Legacy format for backward compatibility
    private List<QuestionAnswer> responses;

    // New structured assessment responses
    private Map<Integer, Integer> phq9Responses; // questionId -> score (0-3)
    private Map<Integer, Integer> bfptResponses; // questionId -> score (1-5) - 50 item Big Five
    private Map<Integer, String> enneagramResponses; // questionId -> "A" or "B"
    private Map<Integer, String> eqResponses; // questionId -> response string
    private Integer eqBatch; // Which EQ batch (1, 2, or 3)

    // Personalized question responses
    private List<PersonalizedResponse> personalizedResponses;

    @Data
    public static class QuestionAnswer {
        private Integer questionId;
        private String question;
        private String answer;
    }

    @Data
    public static class PersonalizedResponse {
        private Integer questionId;
        private String question;
        private String answer;
    }
}
