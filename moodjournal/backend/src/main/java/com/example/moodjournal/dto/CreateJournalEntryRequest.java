package com.example.moodjournal.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateJournalEntryRequest {
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 100, message = "Title must be less than 100 characters")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    @Pattern(regexp = "HAPPY|SAD|ANGRY|NEUTRAL|SURPRISED", message = "Invalid mood")
    private String mood;

    @Pattern(regexp = "PRIVATE|PUBLIC", message = "Invalid visibility")
    private String visibility;

    // Optional AI analysis metadata
    private String analysisEmotion;
    private Double analysisConfidence;
    private Double analysisIntensity;

    // Structured context fields (user-provided)
    private Set<String> contextTags;
    private Integer stressLevel;
    private Integer energyLevel;
    private Integer sleepQuality;
    private String triggerDescription;
}
