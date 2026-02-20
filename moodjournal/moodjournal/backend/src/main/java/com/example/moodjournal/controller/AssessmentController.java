package com.example.moodjournal.controller;

import java.util.List;
import java.util.Map;

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
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.AssessmentSessionRepository;
import com.example.moodjournal.repository.JournalEntryRepository;
import com.example.moodjournal.service.AssessmentService;
import com.example.moodjournal.service.MistralService;
import com.example.moodjournal.service.UserProfileService;
import com.example.moodjournal.service.UserService;




@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {

    private static final Logger log = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    @Autowired
    private MistralService mistralService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    


    @GetMapping("/questions")
    public ResponseEntity<List<AssessmentQuestion>> getQuestions() {
        List<AssessmentQuestion> questions = assessmentService.generateQuestions();
        return ResponseEntity.ok(questions);
    }

    


    @GetMapping("/personalized-questions")
    public ResponseEntity<List<Map<String, Object>>> getPersonalizedQuestions(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        List<JournalEntry> recentEntries = journalEntryRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
        List<Map<String, Object>> questions = mistralService.generatePersonalizedQuestions(recentEntries);
        return ResponseEntity.ok(questions);
    }

    


    @GetMapping("/eq-progress")
    public ResponseEntity<Map<String, Object>> getEQProgress(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        
        List<AssessmentSession> sessions = sessionRepository.findByUserIdOrderByCompletedAtDesc(user.getId());

        int nextBatch = 1;
        int completionPercent = 0;

        if (!sessions.isEmpty()) {
            AssessmentSession lastSession = sessions.get(0);
            Integer eqCompletion = lastSession.getEqCompletionPercent();
            if (eqCompletion != null) {
                completionPercent = eqCompletion;
                if (eqCompletion >= 66) {
                    nextBatch = 3;
                } else if (eqCompletion >= 33) {
                    nextBatch = 2;
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "nextBatch", nextBatch,
                "completionPercent", completionPercent));
    }

    







    @PostMapping("/analyze")
    public ResponseEntity<AnalyzedProfile> analyzeResponses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AssessmentSubmission submission) {

        User user = getUser(userDetails);

        
        AnalyzedProfile profile = assessmentService.analyzeResponses(submission);

        
        try {
            assessmentService.saveAssessmentResults(user, submission, profile);

            log.info("Assessment completed for user {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to persist assessment: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzedProfile.builder()
                            .insights("Analysis failed to save. Please try again.")
                            .build());
        }

        return ResponseEntity.ok(profile);
    }

    


    @GetMapping("/history")
    public ResponseEntity<List<AssessmentSession>> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        java.util.UUID userId = getUser(userDetails).getId();
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
}
