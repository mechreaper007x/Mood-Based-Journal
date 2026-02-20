package com.example.moodjournal.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;





@Data
public class AssessmentSubmission {
    
    private List<QuestionAnswer> responses;

    
    private Map<Integer, Integer> phq9Responses; 
    private Map<Integer, Integer> bfptResponses; 
    private Map<Integer, String> enneagramResponses; 
    private Map<Integer, String> eqResponses; 
    private Integer eqBatch; 

    
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
