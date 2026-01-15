package com.example.moodjournal.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.dto.AnalyzedProfile;
import com.example.moodjournal.dto.AssessmentQuestion;
import com.example.moodjournal.dto.AssessmentSubmission;
import com.example.moodjournal.dto.UserProfileDTO;
import com.example.moodjournal.model.AssessmentResponseItem;
import com.example.moodjournal.model.AssessmentSession;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.AssessmentSessionRepository;
import com.example.moodjournal.service.AssessmentService;
import com.example.moodjournal.service.UserProfileService;
import com.example.moodjournal.service.UserService;

/**
 * REST controller for LLM-powered psychological assessment.
 */
@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {

    private static final Logger log = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserService userService;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    /**
     * Generate 10 psychological assessment questions.
     */
    @GetMapping("/questions")
    public ResponseEntity<List<AssessmentQuestion>> getQuestions() {
        List<AssessmentQuestion> questions = assessmentService.generateQuestions();
        return ResponseEntity.ok(questions);
    }

    /**
     * Analyze submitted responses and update user profile.
     * Also saves the Q&A data to database for future reference.
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzedProfile> analyzeResponses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AssessmentSubmission submission) {

        // Analyze responses
        AnalyzedProfile profile = assessmentService.analyzeResponses(submission);

        // Save session and update profile
        try {
            User user = getUser(userDetails);
            saveAssessmentSession(user, submission, profile);
            updateUserProfile(user.getId(), profile);
        } catch (Exception e) {
            // Log but don't fail - still return the analysis
            log.error("Failed to save assessment session/profile for user: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(profile);
    }

    /**
     * Get user's assessment history.
     */
    @GetMapping("/history")
    public ResponseEntity<List<AssessmentSession>> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUser(userDetails).getId();
        List<AssessmentSession> sessions = sessionRepository.findByUserIdOrderByCompletedAtDesc(userId);
        return ResponseEntity.ok(sessions);
    }

    private User getUser(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user;
        }
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    private void saveAssessmentSession(User user, AssessmentSubmission submission, AnalyzedProfile profile) {
        AssessmentSession session = AssessmentSession.builder()
                .user(user)
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
                .detectedStressors(
                        profile.getDetectedStressors() != null ? String.join(",", profile.getDetectedStressors())
                                : null)
                .insights(profile.getInsights())
                .build();

        // Add all Q&A pairs
        for (var qa : submission.getResponses()) {
            AssessmentResponseItem item = AssessmentResponseItem.builder()
                    .questionNumber(qa.getQuestionId())
                    .questionText(qa.getQuestion())
                    .answerText(qa.getAnswer())
                    .build();
            session.addResponse(item);
        }

        sessionRepository.save(session);
    }

    private void updateUserProfile(Long userId, AnalyzedProfile analyzed) {
        UserProfileDTO profileDTO = userProfileService.getProfileByUserId(userId)
                .orElse(UserProfileDTO.builder().build());

        profileDTO.setExtraversion(analyzed.getExtraversion());
        profileDTO.setAgreeableness(analyzed.getAgreeableness());
        profileDTO.setConscientiousness(analyzed.getConscientiousness());
        profileDTO.setEmotionalStability(analyzed.getEmotionalStability());
        profileDTO.setOpenness(analyzed.getOpenness());
        profileDTO.setPrimaryArchetype(analyzed.getPrimaryArchetype());
        profileDTO.setSecondaryArchetype(analyzed.getSecondaryArchetype());
        profileDTO.setCognitiveEmpathy(analyzed.getCognitiveEmpathy());
        profileDTO.setAffectiveEmpathy(analyzed.getAffectiveEmpathy());
        profileDTO.setCompassionateEmpathy(analyzed.getCompassionateEmpathy());

        if (analyzed.getDetectedStressors() != null) {
            profileDTO.setCurrentStressors(new java.util.HashSet<>(analyzed.getDetectedStressors()));
        }

        // Ensure backend calculated fields are set if missing
        if (profileDTO.getIsComplete() == null) {
            profileDTO.setIsComplete(true);
        }

        userProfileService.saveProfile(userId, profileDTO);
    }
}
