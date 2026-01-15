package com.example.moodjournal.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateJournalEntryRequest {
    @Size(max = 100, message = "Title must be less than 100 characters")
    private String title;

    private String content;
    
    // use String here to avoid Jackson enum binding errors; service will parse
    @Pattern(regexp = "HAPPY|SAD|ANGRY|NEUTRAL|SURPRISED", message = "Invalid mood")
    private String mood;

    @Pattern(regexp = "PRIVATE|PUBLIC", message = "Invalid visibility")
    private String visibility;

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getMood() {
 return mood; }
    public String getVisibility() { return visibility; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setMood(String mood) { this.mood = mood; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
}