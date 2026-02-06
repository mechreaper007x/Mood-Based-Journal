package com.example.moodjournal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String content; // The malicious payload

    @Column(name = "violation_type")
    private String violationType; // e.g., "LAYER_2_REGEX", "LAYER_5_ENTROPY", "LAYER_3_AI"

    @Column(name = "risk_score")
    private Double riskScore; // AI's confidence or severity

    private LocalDateTime timestamp;

    public SecurityEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public SecurityEvent(String content, String violationType, Double riskScore) {
        this.id = UUID.randomUUID();
        this.content = content;
        this.violationType = violationType;
        this.riskScore = riskScore;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
