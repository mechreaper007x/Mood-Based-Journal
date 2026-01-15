package com.example.moodjournal.model;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ===================
    // DEMOGRAPHICS
    // ===================
    private String gender;
    private String employmentStatus; // employed, student, unemployed, retired, self_employed
    private String relationshipStatus; // single, dating, married, divorced, widowed
    private String livingArrangement; // alone, with_family, with_roommates, with_partner

    // ===================
    // BIG 5 PERSONALITY (TIPI scores: 1-7 scale)
    // ===================
    private Integer extraversion;
    private Integer agreeableness;
    private Integer conscientiousness;
    private Integer emotionalStability; // inverse of Neuroticism
    private Integer openness;

    // ===================
    // JUNGIAN ARCHETYPE
    // ===================
    private String primaryArchetype; // hero, caregiver, explorer, rebel, lover, creator, jester, sage, magician,
                                     // ruler, innocent, everyman
    private String secondaryArchetype;

    // ===================
    // EMPATHY QUOTIENT (simplified)
    // ===================
    private Integer cognitiveEmpathy; // 1-10: ability to understand others' perspectives
    private Integer affectiveEmpathy; // 1-10: ability to feel others' emotions
    private Integer compassionateEmpathy; // 1-10: ability to take action to help

    // ===================
    // LIFE CONTEXT
    // ===================
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "user_stressors", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "stressor")
    private Set<String> currentStressors; // work, finances, health, relationships, family, academic, etc.

    private Integer baselineStressLevel; // 1-10
    private Integer baselineEnergyLevel; // 1-10
    private Integer sleepQuality; // 1-10

    // ===================
    // BELIEFS & VALUES (optional)
    // ===================
    @Column(length = 1000)
    private String coreBeliefs; // free text

    @Column(length = 1000)
    private String lifeValues; // free text

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "interest")
    private Set<String> interests; // hobbies, passions

    // ===================
    // OPTIONAL: PAST CONTEXT (sensitive)
    // ===================
    private Boolean hasReportedTrauma;

    @Column(length = 2000)
    private String traumaContext; // optional free text

    // ===================
    // METADATA
    // ===================
    private Boolean isComplete;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isComplete == null) {
            isComplete = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
