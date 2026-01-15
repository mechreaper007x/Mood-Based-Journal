package com.example.moodjournal.model;

import java.time.Instant;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "journal_entry")
public class JournalEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Title cannot be empty")
  @Size(max = 100, message = "Title must be less than 100 characters")
  private String title;

  @Column(columnDefinition = "TEXT")
  @NotBlank(message = "Content cannot be empty")
  private String content;

  @Enumerated(EnumType.STRING)
  @NotNull(message = "Mood is required")
  private Mood mood;

  @Enumerated(EnumType.STRING)
  @NotNull(message = "Visibility is required")
  private Visibility visibility;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @com.fasterxml.jackson.annotation.JsonIgnore
  private User user;

  private Instant createdAt;
  private Instant updatedAt;

  // AI analysis results (optional)
  private String analysisEmotion;
  private Double analysisConfidence;
  private Double analysisIntensity;

  // NLP Pipeline results
  private Double textblobPolarity; // -1 to +1
  private Double vaderCompound; // -1 to +1
  private Double subjectivity; // 0 to 1
  @Column(columnDefinition = "TEXT")
  private String detailedAnalysis; // Gemini narrative

  // ===== STRUCTURED CONTEXT (User-provided) =====
  @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
  @CollectionTable(name = "entry_context_tags", joinColumns = @JoinColumn(name = "entry_id"))
  @Column(name = "tag")
  private Set<String> contextTags; // work, family, health, relationships, self, money

  private Integer stressLevel; // 1-10 at time of writing
  private Integer energyLevel; // 1-10 at time of writing
  private Integer sleepQuality; // 1-5 last night

  @Column(length = 500)
  private String triggerDescription; // "What triggered this feeling?"

  // ===== PROFILE-AWARE ANALYSIS RESULTS =====
  @Column(length = 500)
  private String cognitiveDistortions; // Comma-separated: "catastrophizing,all-or-nothing"

  private Integer riskScore; // 1-10 mental health concern level

  @Column(columnDefinition = "TEXT")
  private String suggestions; // Personalized suggestions JSON

  private String emotionalTrajectory; // improving, declining, stable

  public JournalEntry() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Mood getMood() {
    return mood;
  }

  public void setMood(Mood mood) {
    this.mood = mood;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getAnalysisEmotion() {
    return analysisEmotion;
  }

  public void setAnalysisEmotion(String analysisEmotion) {
    this.analysisEmotion = analysisEmotion;
  }

  public Double getAnalysisConfidence() {
    return analysisConfidence;
  }

  public void setAnalysisConfidence(Double analysisConfidence) {
    this.analysisConfidence = analysisConfidence;
  }

  public Double getAnalysisIntensity() {
    return analysisIntensity;
  }

  public void setAnalysisIntensity(Double analysisIntensity) {
    this.analysisIntensity = analysisIntensity;
  }

  public Double getTextblobPolarity() {
    return textblobPolarity;
  }

  public void setTextblobPolarity(Double textblobPolarity) {
    this.textblobPolarity = textblobPolarity;
  }

  public Double getVaderCompound() {
    return vaderCompound;
  }

  public void setVaderCompound(Double vaderCompound) {
    this.vaderCompound = vaderCompound;
  }

  public Double getSubjectivity() {
    return subjectivity;
  }

  public void setSubjectivity(Double subjectivity) {
    this.subjectivity = subjectivity;
  }

  public String getDetailedAnalysis() {
    return detailedAnalysis;
  }

  public void setDetailedAnalysis(String detailedAnalysis) {
    this.detailedAnalysis = detailedAnalysis;
  }

  // ===== STRUCTURED CONTEXT GETTERS/SETTERS =====
  public Set<String> getContextTags() {
    return contextTags;
  }

  public void setContextTags(Set<String> contextTags) {
    this.contextTags = contextTags;
  }

  public Integer getStressLevel() {
    return stressLevel;
  }

  public void setStressLevel(Integer stressLevel) {
    this.stressLevel = stressLevel;
  }

  public Integer getEnergyLevel() {
    return energyLevel;
  }

  public void setEnergyLevel(Integer energyLevel) {
    this.energyLevel = energyLevel;
  }

  public Integer getSleepQuality() {
    return sleepQuality;
  }

  public void setSleepQuality(Integer sleepQuality) {
    this.sleepQuality = sleepQuality;
  }

  public String getTriggerDescription() {
    return triggerDescription;
  }

  public void setTriggerDescription(String triggerDescription) {
    this.triggerDescription = triggerDescription;
  }

  // ===== GETTERS/SETTERS FOR PROFILE-AWARE ANALYSIS =====
  public String getCognitiveDistortions() {
    return cognitiveDistortions;
  }

  public void setCognitiveDistortions(String cognitiveDistortions) {
    this.cognitiveDistortions = cognitiveDistortions;
  }

  public Integer getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(Integer riskScore) {
    this.riskScore = riskScore;
  }

  public String getSuggestions() {
    return suggestions;
  }

  public void setSuggestions(String suggestions) {
    this.suggestions = suggestions;
  }

  public String getEmotionalTrajectory() {
    return emotionalTrajectory;
  }

  public void setEmotionalTrajectory(String emotionalTrajectory) {
    this.emotionalTrajectory = emotionalTrajectory;
  }

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}

// Sample JSON to create a new JournalEntry
/*
 * {
 * "title": "Day One",
 * "content": "Here's my mood today—fired up!",
 * "mood": "HAPPY",
 * "visibility": "PUBLIC_ANON",
 * "user": {
 * "id": 1
 * }
 * }
 */
