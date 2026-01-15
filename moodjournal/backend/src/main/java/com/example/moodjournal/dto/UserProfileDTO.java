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
    // Demographics
    private String gender;
    private String employmentStatus;
    private String relationshipStatus;
    private String livingArrangement;

    // Big 5 Personality (1-7 scale)
    private Integer extraversion;
    private Integer agreeableness;
    private Integer conscientiousness;
    private Integer emotionalStability;
    private Integer openness;

    // Jungian Archetype
    private String primaryArchetype;
    private String secondaryArchetype;

    // Empathy (1-10 scale)
    private Integer cognitiveEmpathy;
    private Integer affectiveEmpathy;
    private Integer compassionateEmpathy;

    // Life Context
    private Set<String> currentStressors;
    private Integer baselineStressLevel;
    private Integer baselineEnergyLevel;
    private Integer sleepQuality;

    // Beliefs & Values
    private String coreBeliefs;
    private String lifeValues;
    private Set<String> interests;

    // Optional Trauma Context
    private Boolean hasReportedTrauma;
    private String traumaContext;

    // Metadata
    private Boolean isComplete;
}
