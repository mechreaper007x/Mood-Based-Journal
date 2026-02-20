package com.example.moodjournal.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    
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

    
    private Set<String> currentStressors;
    private Integer baselineStressLevel;
    private Integer baselineEnergyLevel;
    private Integer sleepQuality;

    
    private String coreBeliefs;
    private String lifeValues;
    private Set<String> interests;

    
    private Boolean hasReportedTrauma;
    private String traumaContext;

    
    private Boolean isComplete;
}
