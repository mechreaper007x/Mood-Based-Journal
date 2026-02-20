package com.example.moodjournal.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;





@Entity
@Table(name = "assessment_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    private Instant completedAt;

    
    private Integer extraversion;
    private Integer agreeableness;
    private Integer conscientiousness;
    private Integer emotionalStability;
    private Integer openness;

    private String primaryArchetype;
    private String secondaryArchetype;

    private Integer cognitiveEmpathy;
    private Integer affectiveEmpathy;
    private Integer compassionateEmpathy;

    
    private Integer phq9Score;
    private String phq9Severity;

    
    private Integer enneagramType;
    private String enneagramWing;

    
    private Integer eqScore;
    private Integer eqCompletionPercent;

    @Column(length = 500)
    private String detectedStressors; 

    @Column(columnDefinition = "TEXT")
    private String insights;

    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<AssessmentResponseItem> responses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.completedAt = Instant.now();
    }

    public void addResponse(AssessmentResponseItem response) {
        responses.add(response);
        response.setSession(this);
    }
}
