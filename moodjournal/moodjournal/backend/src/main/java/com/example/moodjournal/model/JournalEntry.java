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

import jakarta.persistence.Convert;
import com.example.moodjournal.security.crypto.EncryptedStringConverter;

@Entity
@Table(name = "journal_entry")
public class JournalEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @jakarta.persistence.Column(nullable = false)
  @jakarta.validation.constraints.NotBlank(message = "Title cannot be empty")
  @jakarta.validation.constraints.Size(max = 100, message = "Title must be less than 100 characters")
  @jakarta.persistence.Convert(converter = com.example.moodjournal.util.AttributeEncryptor.class)
  private String title;

  @jakarta.persistence.Column(nullable = false, columnDefinition = "TEXT")
  @jakarta.validation.constraints.NotBlank(message = "Content cannot be empty")
  @jakarta.persistence.Convert(converter = com.example.moodjournal.util.AttributeEncryptor.class)
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

  
  private String analysisEmotion;
  private Double analysisConfidence;
  private Double analysisIntensity;

  
  private Double textblobPolarity; 
  private Double vaderCompound; 
  private Double subjectivity; 
  @Column(columnDefinition = "TEXT")
  private String detailedAnalysis; 

  
  @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
  @CollectionTable(name = "entry_context_tags", joinColumns = @JoinColumn(name = "entry_id"))
  @Column(name = "tag")
  private Set<String> contextTags; 

  private Integer stressLevel; 
  private Integer energyLevel; 
  private Integer sleepQuality; 

  @Column(length = 500)
  private String triggerDescription; 

  
  @Column(length = 500)
  private String cognitiveDistortions; 

  private Integer riskScore; 

  @Column(columnDefinition = "TEXT")
  private String suggestions; 

  private String emotionalTrajectory; 

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













