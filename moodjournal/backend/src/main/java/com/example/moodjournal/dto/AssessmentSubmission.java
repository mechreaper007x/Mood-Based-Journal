package com.example.moodjournal.dto;

import java.util.List;

import lombok.Data;

/**
 * User's responses to assessment questions.
 */
@Data
public class AssessmentSubmission {
    private List<QuestionAnswer> responses;

    @Data
    public static class QuestionAnswer {
        private Integer questionId;
        private String question;
        private String answer;
    }
}
