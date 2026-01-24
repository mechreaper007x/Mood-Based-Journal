package com.example.moodjournal.dto;

import lombok.Data;

@Data
public class GoalRequest {
    private String title;
    private String description;
    private String category;
    private String targetDate;
    private Integer progress;
    private Boolean isCompleted;
}
