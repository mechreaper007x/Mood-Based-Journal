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

    /**
     * Get profile for a user, or return empty if not exists
     */
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getProfileByUserId(Long userId) {
        log.debug("Fetching profile for userId: {}", userId);
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        log.debug("Profile found: {}", profileOpt.isPresent());

        // Force initialize lazy collections within transaction
        profileOpt.ifPresent(profile -> {
            if (profile.getCurrentStressors() != null) {
                profile.getCurrentStressors().size(); // force init
            }
            if (profile.getInterests() != null) {
                profile.getInterests().size(); // force init
            }
        });

        return profileOpt.map(this::toDTO);
    }

    /**
     * Check if a user has a completed profile
     */
    public boolean isProfileComplete(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> Boolean.TRUE.equals(profile.getIsComplete()))
                .orElse(false);
    }

    /**
     * Check if profile exists for a user
     */
    public boolean profileExists(Long userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    /**
     * Create or update user profile
     */
    @Transactional
    public UserProfileDTO saveProfile(Long userId, UserProfileDTO dto) {
        log.info("Saving profile for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(new UserProfile());

        boolean isNew = profile.getId() == null;
        log.info("Profile {} for userId: {}", isNew ? "creating new" : "updating existing", userId);

        // Set user reference
        profile.setUser(user);

        // Map DTO to entity
        updateProfileFromDTO(profile, dto);

        // Save and return
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile saved successfully with id: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * Mark profile as complete
     */
    @Transactional
    public void markProfileComplete(Long userId) {
        userProfileRepository.findByUserId(userId)
                .ifPresent(profile -> {
                    profile.setIsComplete(true);
                    userProfileRepository.save(profile);
                });
    }

    // ================
    // HELPER METHODS
    // ================

    private void updateProfileFromDTO(UserProfile profile, UserProfileDTO dto) {
        // Demographics
        profile.setGender(dto.getGender());
        profile.setEmploymentStatus(dto.getEmploymentStatus());
        profile.setRelationshipStatus(dto.getRelationshipStatus());
        profile.setLivingArrangement(dto.getLivingArrangement());

        // Big 5
        profile.setExtraversion(dto.getExtraversion());
        profile.setAgreeableness(dto.getAgreeableness());
        profile.setConscientiousness(dto.getConscientiousness());
        profile.setEmotionalStability(dto.getEmotionalStability());
        profile.setOpenness(dto.getOpenness());

        // Jungian
        profile.setPrimaryArchetype(dto.getPrimaryArchetype());
        profile.setSecondaryArchetype(dto.getSecondaryArchetype());

        // Empathy
        profile.setCognitiveEmpathy(dto.getCognitiveEmpathy());
        profile.setAffectiveEmpathy(dto.getAffectiveEmpathy());
        profile.setCompassionateEmpathy(dto.getCompassionateEmpathy());

        // Life Context
        profile.setCurrentStressors(dto.getCurrentStressors());
        profile.setBaselineStressLevel(dto.getBaselineStressLevel());
        profile.setBaselineEnergyLevel(dto.getBaselineEnergyLevel());
        profile.setSleepQuality(dto.getSleepQuality());

        // Beliefs
        profile.setCoreBeliefs(dto.getCoreBeliefs());
        profile.setLifeValues(dto.getLifeValues());
        profile.setInterests(dto.getInterests());

        // Trauma
        profile.setHasReportedTrauma(dto.getHasReportedTrauma());
        profile.setTraumaContext(dto.getTraumaContext());

        // Complete status
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
