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

    
    
    
    private String gender;
    private String employmentStatus; 
    private String relationshipStatus; 
    private String livingArrangement; 

    
    
    
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

    
    
    
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "user_stressors", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "stressor")
    private Set<String> currentStressors; 

    private Integer baselineStressLevel; 
    private Integer baselineEnergyLevel; 
    private Integer sleepQuality; 

    
    
    
    @Column(length = 1000)
    private String coreBeliefs; 

    @Column(length = 1000)
    private String lifeValues; 

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "interest")
    private Set<String> interests; 

    
    
    
    private Boolean hasReportedTrauma;

    @Column(length = 2000)
    private String traumaContext; 

    
    
    
    private Boolean isComplete;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @jakarta.persistence.Version
    private Long version;

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
