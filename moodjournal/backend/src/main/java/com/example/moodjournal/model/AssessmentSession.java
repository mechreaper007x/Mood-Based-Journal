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

/**
 * Represents a completed deep psychological assessment session.
 * Contains all Q&A pairs and the analyzed results.
 */
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

    // Analyzed results stored for quick access
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

    @Column(length = 500)
    private String detectedStressors; // Comma-separated

    @Column(columnDefinition = "TEXT")
    private String insights;

    // All Q&A pairs from this session
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
