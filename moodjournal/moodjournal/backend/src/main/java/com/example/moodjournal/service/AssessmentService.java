package com.example.moodjournal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.moodjournal.dto.AnalyzedProfile;
import com.example.moodjournal.dto.AssessmentQuestion;
import com.example.moodjournal.dto.AssessmentSubmission;
import com.example.moodjournal.dto.CrisisFlag;
import com.example.moodjournal.dto.UserProfileDTO;
import com.example.moodjournal.model.AssessmentResponseItem;
import com.example.moodjournal.model.AssessmentSession;
import com.example.moodjournal.model.CachedQuestionSet;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.AssessmentSessionRepository;
import com.example.moodjournal.repository.CachedQuestionSetRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;






@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);
    private static final int MAX_CACHED_SETS = 5; 

    @Autowired
    private AISecurityService securityService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private CachedQuestionSetRepository questionSetRepository;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    @Autowired
    private UserProfileService userProfileService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    



    public List<AssessmentQuestion> generateQuestions() {
        
        long cachedCount = questionSetRepository.count();

        
        if (cachedCount >= MAX_CACHED_SETS) {
            Optional<CachedQuestionSet> cached = questionSetRepository.findRandom();
            if (cached.isPresent()) {
                try {
                    CachedQuestionSet set = cached.get();
                    set.incrementUsage();
                    questionSetRepository.save(set);
                    log.info("Using cached question set #{} (usage: {})", set.getId(), set.getUsageCount());
                    return objectMapper.readValue(
                            set.getQuestionsJson(),
                            new TypeReference<List<AssessmentQuestion>>() {
                            });
                } catch (Exception e) {
                    log.error("Failed to parse cached questions: {}", e.getMessage());
                }
            }
        }

        
        log.info("Generating new question set via LLM (cached: {})", cachedCount);
        return generateAndCacheQuestions();
    }

    


    private List<AssessmentQuestion> generateAndCacheQuestions() {
        String prompt = """
                You are a licensed clinical psychologist with 20 years of experience in personality assessment
                and psychological profiling. Your task is to create a deep psychological assessment.

                Generate exactly 10 open-ended questions that will help you understand a person's:
                - Core personality traits (Big Five: Extraversion, Agreeableness, Conscientiousness, Emotional Stability, Openness)
                - Psychological archetype (Hero, Caregiver, Explorer, Rebel, Lover, Creator, Jester, Sage, Magician, Ruler, Innocent, Everyman)
                - Current life stressors and emotional state
                - Empathy style (cognitive, affective, compassionate)
                - Values and belief systems

                Create questions that are:
                - Open-ended and invite reflection
                - Non-judgmental and safe to answer honestly
                - Revealing of deeper psychological patterns
                - Varied in focus (some about past, present, relationships, self-perception)

                IMPORTANT: You MUST generate exactly 10 questions.
                For questions 1 through 9, they MUST correspond to Enneagram Types 1 through 9 in order.
                ID 1 -> Type 1, ID 2 -> Type 2, ... ID 9 -> Type 9.
                ID 10 -> General Reflection.

                Return ONLY a JSON array, no markdown, no explanation:
                [
                  {
                    "id": 1,
                    "scale": "ENNEAGRAM_TYPE_1",
                    "question": "..."
                  },
                  ...
                ]
                """;

        try {
            String response = geminiService.callGeminiWithRotation(prompt);
            String cleanJson = geminiService.cleanJsonResponse(response);
            log.info("Generated assessment questions: {}", cleanJson);

            List<AssessmentQuestion> questions = objectMapper.readValue(
                    cleanJson,
                    new TypeReference<List<AssessmentQuestion>>() {
                    });

            
            CachedQuestionSet newSet = CachedQuestionSet.builder()
                    .questionsJson(cleanJson)
                    .build();
            questionSetRepository.save(newSet);
            log.info("Cached new question set (total cached: {})", questionSetRepository.count());

            return questions;
        } catch (Exception e) {
            log.error("Failed to generate questions: {}", e.getMessage(), e);
            return getFallbackQuestions();
        }
    }

    




    public AnalyzedProfile analyzeResponses(AssessmentSubmission submission) {
        AnalyzedProfile.AnalyzedProfileBuilder builder = AnalyzedProfile.builder();

        
        if (submission.getPhq9Responses() != null && !submission.getPhq9Responses().isEmpty()) {
            int phq9Score = scorePHQ9(submission.getPhq9Responses());
            String severity = getPHQ9Severity(phq9Score);
            builder.phq9Score(phq9Score).phq9Severity(severity);
        }

        
        if (submission.getBfptResponses() != null && !submission.getBfptResponses().isEmpty()) {
            Map<String, Integer> big5 = scoreBFPT(submission.getBfptResponses());
            builder.extraversion(big5.get("extraversion"))
                    .agreeableness(big5.get("agreeableness"))
                    .conscientiousness(big5.get("conscientiousness"))
                    .emotionalStability(big5.get("emotionalStability"))
                    .openness(big5.get("openness"));
        }

        
        if (submission.getEnneagramResponses() != null && !submission.getEnneagramResponses().isEmpty()) {
            int[] enneagram = scoreEnneagram(submission.getEnneagramResponses());
            builder.enneagramType(enneagram[0]).enneagramWing(String.valueOf(enneagram[1]));
        }

        
        if (submission.getEqResponses() != null && !submission.getEqResponses().isEmpty()) {
            Map<String, Integer> eq = scoreEQ60(submission.getEqResponses());
            int completionPercent = submission.getEqBatch() != null ? submission.getEqBatch() * 33 : 33;
            builder.eqScore(eq.get("total"))
                    .eqCompletionPercent(Math.min(completionPercent, 100))
                    .cognitiveEmpathy(normalizeEQ(eq.get("cognitive"), 44, 10)) 
                    .affectiveEmpathy(normalizeEQ(eq.get("affective"), 12, 10)) 
                    .compassionateEmpathy(normalizeEQ(eq.get("compassionate"), 24, 10)); 
        }

        
        String insights = generateInsights(submission);
        builder.insights(insights);

        
        AnalyzedProfile profile = builder.build();
        if (profile.getExtraversion() == null)
            profile.setExtraversion(4);
        if (profile.getAgreeableness() == null)
            profile.setAgreeableness(4);
        if (profile.getConscientiousness() == null)
            profile.setConscientiousness(4);
        if (profile.getEmotionalStability() == null)
            profile.setEmotionalStability(4);
        if (profile.getOpenness() == null)
            profile.setOpenness(4);
        if (profile.getPrimaryArchetype() == null)
            profile.setPrimaryArchetype("sage");
        if (profile.getSecondaryArchetype() == null)
            profile.setSecondaryArchetype("explorer");
        if (profile.getCognitiveEmpathy() == null)
            profile.setCognitiveEmpathy(5);
        if (profile.getAffectiveEmpathy() == null)
            profile.setAffectiveEmpathy(5);
        if (profile.getCompassionateEmpathy() == null)
            profile.setCompassionateEmpathy(5);
        if (profile.getDetectedStressors() == null)
            profile.setDetectedStressors(List.of());

        return profile;
    }

    


    private int scorePHQ9(Map<Integer, Integer> responses) {
        if (responses == null || responses.isEmpty()) {
            return 0; 
        }

        int sum = responses.values().stream()
                .filter(java.util.Objects::nonNull)
                .mapToInt(v -> {
                    if (v < 0 || v > 3)
                        return 0; 
                    return v;
                })
                .sum();

        
        return Math.min(27, Math.max(0, sum));
    }

    


    private String getPHQ9Severity(int score) {
        if (score <= 4)
            return "None-minimal";
        if (score <= 9)
            return "Mild";
        if (score <= 14)
            return "Moderate";
        if (score <= 19)
            return "Moderately severe";
        return "Severe";
    }

    












    public CrisisFlag checkPHQ9SuicideRisk(Map<Integer, Integer> phq9Responses) {
        if (phq9Responses == null || phq9Responses.isEmpty()) {
            return CrisisFlag.none();
        }

        
        Integer item9Score = phq9Responses.get(8);

        if (item9Score == null || item9Score <= 0) {
            return CrisisFlag.none();
        }

        log.warn("CRISIS FLAG: PHQ-9 Item 9 triggered with score={}", item9Score);
        return CrisisFlag.triggered(item9Score);
    }

    










    private Map<String, Integer> scoreBFPT(Map<Integer, Integer> r) {
        
        java.util.function.Function<Integer, Integer> get = id -> r.getOrDefault(id, 3);

        
        int rawE = 20 + get.apply(1) - get.apply(6) + get.apply(11) - get.apply(16)
                + get.apply(21) - get.apply(26) + get.apply(31) - get.apply(36)
                + get.apply(41) - get.apply(46);

        int rawA = 14 - get.apply(2) + get.apply(7) - get.apply(12) + get.apply(17)
                - get.apply(22) + get.apply(27) - get.apply(32) + get.apply(37)
                + get.apply(42) + get.apply(47);

        int rawC = 14 + get.apply(3) - get.apply(8) + get.apply(13) - get.apply(18)
                + get.apply(23) - get.apply(28) + get.apply(33) - get.apply(38)
                + get.apply(43) + get.apply(48);

        
        int rawN = 38 - get.apply(4) + get.apply(9) - get.apply(14) + get.apply(19)
                - get.apply(24) - get.apply(29) - get.apply(34) - get.apply(39)
                - get.apply(44) - get.apply(49);

        int rawO = 8 + get.apply(5) - get.apply(10) + get.apply(15) - get.apply(20)
                + get.apply(25) - get.apply(30) + get.apply(35) + get.apply(40)
                + get.apply(45) + get.apply(50);

        
        
        int emotionalStability = 8 - normalize(rawN, 0, 40, 1, 7);

        return Map.of(
                "extraversion", normalize(rawE, 0, 40, 1, 7),
                "agreeableness", normalize(rawA, 0, 40, 1, 7),
                "conscientiousness", normalize(rawC, 0, 40, 1, 7),
                "emotionalStability", Math.max(1, Math.min(7, emotionalStability)),
                "openness", normalize(rawO, 0, 40, 1, 7));
    }

    


    private int normalize(int value, int oldMin, int oldMax, int newMin, int newMax) {
        value = Math.max(oldMin, Math.min(oldMax, value));
        double ratio = (double) (value - oldMin) / (oldMax - oldMin);
        return (int) Math.round(newMin + ratio * (newMax - newMin));
    }

    


    private int normalizeEQ(int raw, int maxRaw, int maxScale) {
        if (raw <= 0)
            return 1;
        double ratio = (double) raw / maxRaw;
        return Math.max(1, Math.min(maxScale, (int) Math.round(ratio * maxScale)));
    }

    


    private int[] scoreEnneagram(Map<Integer, String> responses) {
        
        int[] typeCounts = new int[10]; 

        
        
        for (var entry : responses.entrySet()) {
            
            
            
            
            
            
            
            
            
            
            

            
            
            
            
            

            
            
            int id = entry.getKey();
            int type = id % 9;
            if (type == 0)
                type = 9;

            
            if (type >= 1 && type <= 9) {
                typeCounts[type]++;
            }
        }

        
        int maxType = 1;
        int maxCount = typeCounts[1];
        for (int i = 2; i <= 9; i++) {
            if (typeCounts[i] > maxCount) {
                maxCount = typeCounts[i];
                maxType = i;
            }
        }

        
        int leftWing = maxType == 1 ? 9 : maxType - 1;
        int rightWing = maxType == 9 ? 1 : maxType + 1;
        int wing = typeCounts[leftWing] >= typeCounts[rightWing] ? leftWing : rightWing;

        return new int[] { maxType, wing };
    }

    



    












    private Map<String, Integer> scoreEQ60(Map<Integer, String> responses) {
        int cognitive = 0, affective = 0, compassionate = 0;

        
        
        
        Map<Integer, String[]> questionMeta = Map.ofEntries(
                Map.entry(1, new String[] { "C", "+" }), Map.entry(2, new String[] { "D", "" }),
                Map.entry(3, new String[] { "D", "" }),
                Map.entry(4, new String[] { "C", "-" }), Map.entry(5, new String[] { "D", "" }),
                Map.entry(6, new String[] { "P", "+" }),
                Map.entry(7, new String[] { "D", "" }), Map.entry(8, new String[] { "C", "-" }),
                Map.entry(9, new String[] { "D", "" }),
                Map.entry(10, new String[] { "C", "-" }), Map.entry(11, new String[] { "P", "-" }),
                Map.entry(12, new String[] { "P", "-" }),
                Map.entry(13, new String[] { "D", "" }), Map.entry(14, new String[] { "C", "-" }),
                Map.entry(15, new String[] { "C", "-" }),
                Map.entry(16, new String[] { "D", "" }), Map.entry(17, new String[] { "D", "" }),
                Map.entry(18, new String[] { "A", "-" }),
                Map.entry(19, new String[] { "C", "+" }), Map.entry(20, new String[] { "D", "" }),
                Map.entry(21, new String[] { "A", "-" }),
                Map.entry(22, new String[] { "C", "+" }), Map.entry(23, new String[] { "D", "" }),
                Map.entry(24, new String[] { "D", "" }),
                Map.entry(25, new String[] { "C", "+" }), Map.entry(26, new String[] { "C", "+" }),
                Map.entry(27, new String[] { "P", "-" }),
                Map.entry(28, new String[] { "P", "-" }), Map.entry(29, new String[] { "C", "-" }),
                Map.entry(30, new String[] { "D", "" }),
                Map.entry(31, new String[] { "D", "" }), Map.entry(32, new String[] { "A", "-" }),
                Map.entry(33, new String[] { "D", "" }),
                Map.entry(34, new String[] { "C", "-" }), Map.entry(35, new String[] { "C", "+" }),
                Map.entry(36, new String[] { "C", "+" }),
                Map.entry(37, new String[] { "P", "+" }), Map.entry(38, new String[] { "A", "+" }),
                Map.entry(39, new String[] { "A", "-" }),
                Map.entry(40, new String[] { "D", "" }), Map.entry(41, new String[] { "C", "+" }),
                Map.entry(42, new String[] { "A", "+" }),
                Map.entry(43, new String[] { "P", "+" }), Map.entry(44, new String[] { "C", "+" }),
                Map.entry(45, new String[] { "D", "" }),
                Map.entry(46, new String[] { "P", "-" }), Map.entry(47, new String[] { "D", "" }),
                Map.entry(48, new String[] { "C", "-" }),
                Map.entry(49, new String[] { "P", "-" }), Map.entry(50, new String[] { "A", "-" }),
                Map.entry(51, new String[] { "D", "" }),
                Map.entry(52, new String[] { "C", "+" }), Map.entry(53, new String[] { "D", "" }),
                Map.entry(54, new String[] { "C", "+" }),
                Map.entry(55, new String[] { "C", "+" }), Map.entry(56, new String[] { "D", "" }),
                Map.entry(57, new String[] { "C", "+" }),
                Map.entry(58, new String[] { "C", "+" }), Map.entry(59, new String[] { "P", "+" }),
                Map.entry(60, new String[] { "C", "+" }));

        for (var entry : responses.entrySet()) {
            int questionId = entry.getKey();
            String response = entry.getValue();
            String[] meta = questionMeta.get(questionId);

            if (meta == null || "D".equals(meta[0]))
                continue; 

            int points = 0;
            boolean isPositive = "+".equals(meta[1]);

            if (isPositive) {
                
                if ("strongly_agree".equals(response))
                    points = 2;
                else if ("slightly_agree".equals(response))
                    points = 1;
            } else {
                
                if ("strongly_disagree".equals(response))
                    points = 2;
                else if ("slightly_disagree".equals(response))
                    points = 1;
            }

            
            switch (meta[0]) {
                case "C" -> cognitive += points;
                case "A" -> affective += points;
                case "P" -> compassionate += points;
            }
        }

        return Map.of(
                "cognitive", cognitive,
                "affective", affective,
                "compassionate", compassionate,
                "total", cognitive + affective + compassionate);
    }

    


    private String generateInsights(AssessmentSubmission submission) {
        StringBuilder context = new StringBuilder();

        if (submission.getPhq9Responses() != null) {
            int score = scorePHQ9(submission.getPhq9Responses());
            context.append("PHQ-9 Score: ").append(score).append(" (").append(getPHQ9Severity(score)).append(")\n");
        }
        if (submission.getBfptResponses() != null) {
            Map<String, Integer> big5 = scoreBFPT(submission.getBfptResponses());
            context.append("Big 5: E=").append(big5.get("extraversion"))
                    .append(" A=").append(big5.get("agreeableness"))
                    .append(" C=").append(big5.get("conscientiousness"))
                    .append(" ES=").append(big5.get("emotionalStability"))
                    .append(" O=").append(big5.get("openness")).append("\n");
        }
        if (submission.getEnneagramResponses() != null) {
            int[] en = scoreEnneagram(submission.getEnneagramResponses());
            context.append("Enneagram: Type ").append(en[0]).append("w").append(en[1]).append("\n");
        }
        if (submission.getPersonalizedResponses() != null) {
            for (var pr : submission.getPersonalizedResponses()) {
                context.append("Q: ").append(pr.getQuestion()).append("\n");

                
                String safeAnswer = "Redacted";
                try {
                    safeAnswer = securityService.securePrompt(pr.getAnswer());
                } catch (SecurityException se) {
                    log.warn("Assessment blocked by security policy: {}", se.getMessage());
                    throw new SecurityException(
                            "One or more assessment responses contain unsafe instructions. Please revise and retry.");
                } catch (Exception e) {
                    log.error("Assessment answer parsing/security preprocessing failed.", e);
                    safeAnswer = "[REDACTED - UNPROCESSABLE ANSWER]";
                }
                context.append("A: ").append(safeAnswer).append("\n\n");
            }
        }

        if (context.isEmpty()) {
            return "Assessment data incomplete. Please complete all sections for detailed insights.";
        }

        String prompt = """
                You are a clinical psychologist. Based on this assessment data, provide a brief 2-3 sentence
                personalized psychological insight. Be warm, non-judgmental, and focus on growth opportunities.

                %s
                """.formatted(context.toString());

        try {
            String response = geminiService.callGeminiWithRotation(prompt);
            return geminiService.cleanJsonResponse(response).replaceAll("\"", "");
        } catch (Exception e) {
            log.error("Failed to generate insights: {}", e.getMessage());
            return "Your assessment reveals a unique psychological profile. Continue journaling for deeper insights.";
        }
    }

    
    private Integer getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number)
            return ((Number) val).intValue();
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (Exception e) {
            }
        }
        return defaultVal;
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List)
            return (List<String>) val;
        return new ArrayList<>();
    }

    private List<AssessmentQuestion> getFallbackQuestions() {
        return List.of(
                new AssessmentQuestion(1,
                        "Describe a moment when you felt truly alive and engaged. What were you doing?", "GENERAL"),
                new AssessmentQuestion(2, "How do you typically react when facing unexpected challenges or setbacks?",
                        "GENERAL"),
                new AssessmentQuestion(3, "What qualities do you most admire in others, and why?", "GENERAL"),
                new AssessmentQuestion(4, "Describe your ideal way to spend a free day with no obligations.",
                        "GENERAL"),
                new AssessmentQuestion(5, "When you disagree with someone close to you, how do you usually handle it?",
                        "GENERAL"),
                new AssessmentQuestion(6, "What recurring worries or thoughts occupy your mind most often?", "GENERAL"),
                new AssessmentQuestion(7, "How would your closest friend describe your personality in three words?",
                        "GENERAL"),
                new AssessmentQuestion(8,
                        "Describe a decision you made that went against others' expectations. How did it feel?",
                        "GENERAL"),
                new AssessmentQuestion(9,
                        "When someone you care about is going through difficulty, what's your instinct?", "GENERAL"),
                new AssessmentQuestion(10,
                        "What do you believe is your greatest personal strength and your biggest challenge?",
                        "GENERAL"));
    }

    private AnalyzedProfile getDefaultProfile() {
        return AnalyzedProfile.builder()
                .extraversion(4).agreeableness(4).conscientiousness(4)
                .emotionalStability(4).openness(4)
                .primaryArchetype("sage").secondaryArchetype("explorer")
                .cognitiveEmpathy(5).affectiveEmpathy(5).compassionateEmpathy(5)
                .detectedStressors(List.of())
                .insights("Unable to generate detailed analysis. Please try again.")
                .build();
    }

    





    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public AssessmentSession saveAssessmentResults(User user, AssessmentSubmission submission,
            AnalyzedProfile profile) {

        
        Optional<AssessmentSession> lastSession = sessionRepository.findTopByUserIdOrderByCompletedAtDesc(user.getId());
        if (lastSession.isPresent()) {
            java.time.Instant now = java.time.Instant.now();
            java.time.Instant lastTime = lastSession.get().getCompletedAt();
            
            if (lastTime != null && lastTime.plusSeconds(60).isAfter(now)) {
                log.warn("Duplicate assessment submission avoided for user {}", user.getId());
                return lastSession.get();
            }
        }

        
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
                .phq9Score(profile.getPhq9Score())
                .phq9Severity(profile.getPhq9Severity())
                .enneagramType(profile.getEnneagramType())
                .enneagramWing(profile.getEnneagramWing())
                .eqScore(profile.getEqScore())
                .eqCompletionPercent(profile.getEqCompletionPercent())
                .detectedStressors(
                        profile.getDetectedStressors() != null ? String.join(",", profile.getDetectedStressors())
                                : null)
                .insights(profile.getInsights())
                .build();

        
        if (submission.getResponses() != null) {
            for (var qa : submission.getResponses()) {
                AssessmentResponseItem item = AssessmentResponseItem.builder()
                        .questionNumber(qa.getQuestionId())
                        .questionText(qa.getQuestion())
                        .answerText(qa.getAnswer())
                        .build();
                session.addResponse(item);
            }
        }

        AssessmentSession savedSession = sessionRepository.save(session);

        
        updateUserProfile(user.getId(), profile);

        return savedSession;
    }

    private void updateUserProfile(java.util.UUID userId, AnalyzedProfile analyzed) {
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

        
        if (profileDTO.getIsComplete() == null) {
            profileDTO.setIsComplete(true);
        }

        userProfileService.saveProfile(userId, profileDTO);
    }
}
