package com.example.moodjournal.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moodjournal.dto.UserProfileDTO;
import com.example.moodjournal.model.User;
import com.example.moodjournal.model.UserProfile;
import com.example.moodjournal.repository.UserProfileRepository;
import com.example.moodjournal.repository.UserRepository;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    


    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getProfileByUserId(java.util.UUID userId) {
        log.debug("Fetching profile for userId: {}", userId);
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        log.debug("Profile found: {}", profileOpt.isPresent());

        
        profileOpt.ifPresent(profile -> {
            if (profile.getCurrentStressors() != null) {
                profile.getCurrentStressors().size(); 
            }
            if (profile.getInterests() != null) {
                profile.getInterests().size(); 
            }
        });

        return profileOpt.map(this::toDTO);
    }

    


    public boolean isProfileComplete(java.util.UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> Boolean.TRUE.equals(profile.getIsComplete()))
                .orElse(false);
    }

    


    public boolean profileExists(java.util.UUID userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    


    @Transactional
    public UserProfileDTO saveProfile(java.util.UUID userId, UserProfileDTO dto) {
        log.info("Saving profile for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(new UserProfile());

        boolean isNew = profile.getId() == null;
        log.info("Profile {} for userId: {}", isNew ? "creating new" : "updating existing", userId);

        
        profile.setUser(user);

        
        updateProfileFromDTO(profile, dto);

        
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile saved successfully with id: {}", saved.getId());
        return toDTO(saved);
    }

    


    @Transactional
    public void markProfileComplete(java.util.UUID userId) {
        userProfileRepository.findByUserId(userId)
                .ifPresent(profile -> {
                    profile.setIsComplete(true);
                    userProfileRepository.save(profile);
                });
    }

    
    
    

    private void updateProfileFromDTO(UserProfile profile, UserProfileDTO dto) {
        
        profile.setGender(dto.getGender());
        profile.setEmploymentStatus(dto.getEmploymentStatus());
        profile.setRelationshipStatus(dto.getRelationshipStatus());
        profile.setLivingArrangement(dto.getLivingArrangement());

        
        profile.setExtraversion(dto.getExtraversion());
        profile.setAgreeableness(dto.getAgreeableness());
        profile.setConscientiousness(dto.getConscientiousness());
        profile.setEmotionalStability(dto.getEmotionalStability());
        profile.setOpenness(dto.getOpenness());

        
        profile.setPrimaryArchetype(dto.getPrimaryArchetype());
        profile.setSecondaryArchetype(dto.getSecondaryArchetype());

        
        profile.setCognitiveEmpathy(dto.getCognitiveEmpathy());
        profile.setAffectiveEmpathy(dto.getAffectiveEmpathy());
        profile.setCompassionateEmpathy(dto.getCompassionateEmpathy());

        
        profile.setCurrentStressors(dto.getCurrentStressors());
        profile.setBaselineStressLevel(dto.getBaselineStressLevel());
        profile.setBaselineEnergyLevel(dto.getBaselineEnergyLevel());
        profile.setSleepQuality(dto.getSleepQuality());

        
        profile.setCoreBeliefs(dto.getCoreBeliefs());
        profile.setLifeValues(dto.getLifeValues());
        profile.setInterests(dto.getInterests());

        
        profile.setHasReportedTrauma(dto.getHasReportedTrauma());
        profile.setTraumaContext(dto.getTraumaContext());

        
        if (dto.getIsComplete() != null) {
            profile.setIsComplete(dto.getIsComplete());
        }
    }

    private UserProfileDTO toDTO(UserProfile profile) {
        try {
            log.debug("Converting profile id={} to DTO", profile.getId());
            return UserProfileDTO.builder()
                    .gender(profile.getGender())
                    .employmentStatus(profile.getEmploymentStatus())
                    .relationshipStatus(profile.getRelationshipStatus())
                    .livingArrangement(profile.getLivingArrangement())
                    .extraversion(profile.getExtraversion())
                    .agreeableness(profile.getAgreeableness())
                    .conscientiousness(profile.getConscientiousness())
                    .emotionalStability(profile.getEmotionalStability())
                    .openness(profile.getOpenness())
                    .primaryArchetype(profile.getPrimaryArchetype())
                    .secondaryArchetype(profile.getSecondaryArchetype())
                    .cognitiveEmpathy(profile.getCognitiveEmpathy())
                    .affectiveEmpathy(profile.getAffectiveEmpathy())
                    .compassionateEmpathy(profile.getCompassionateEmpathy())
                    .currentStressors(profile.getCurrentStressors())
                    .baselineStressLevel(profile.getBaselineStressLevel())
                    .baselineEnergyLevel(profile.getBaselineEnergyLevel())
                    .sleepQuality(profile.getSleepQuality())
                    .coreBeliefs(profile.getCoreBeliefs())
                    .lifeValues(profile.getLifeValues())
                    .interests(profile.getInterests())
                    .hasReportedTrauma(profile.getHasReportedTrauma())
                    .traumaContext(profile.getTraumaContext())
                    .isComplete(profile.getIsComplete())
                    .build();
        } catch (Exception e) {
            log.error("Error converting profile to DTO: {}", e.getMessage(), e);
            throw e;
        }
    }
}
