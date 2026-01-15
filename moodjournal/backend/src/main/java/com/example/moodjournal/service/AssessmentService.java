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
import com.example.moodjournal.model.CachedQuestionSet;
import com.example.moodjournal.repository.CachedQuestionSetRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for LLM-powered psychological assessment.
 * Uses Gemini with a professional psychologist persona.
 * Caches question sets to reduce LLM calls.
 */
@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);
    private static final int MAX_CACHED_SETS = 5; // Keep up to 5 different question sets

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private CachedQuestionSetRepository questionSetRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate 10 psychological assessment questions.
     * Uses caching to avoid redundant LLM calls.
     */
    public List<AssessmentQuestion> generateQuestions() {
        // Check if we have cached questions
        long cachedCount = questionSetRepository.count();

        // If we have enough cached sets, use one randomly
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

        // Generate new questions via LLM
        log.info("Generating new question set via LLM (cached: {})", cachedCount);
        return generateAndCacheQuestions();
    }

    /**
     * Generate new questions from LLM and cache them.
     */
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

                Return ONLY a JSON array, no markdown, no explanation:
                [
                  {"id": 1, "question": "..."},
                  {"id": 2, "question": "..."},
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

            // Cache the new question set
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

    /**
     * Analyze user's responses and derive psychological profile.
     */
    public AnalyzedProfile analyzeResponses(AssessmentSubmission submission) {
        StringBuilder qaPairs = new StringBuilder();
        for (var qa : submission.getResponses()) {
            qaPairs.append("Q").append(qa.getQuestionId()).append(": ").append(qa.getQuestion()).append("\n");
            qaPairs.append("A: ").append(qa.getAnswer()).append("\n\n");
        }

        String prompt = """
                You are a licensed clinical psychologist with 20 years of experience in personality assessment.
                You have conducted a psychological assessment and received the following responses:

                --- ASSESSMENT RESPONSES ---
                %s
                --- END RESPONSES ---

                Based on these responses, provide a comprehensive psychological profile analysis.

                Analyze carefully for:
                1. Big Five personality traits (rate each 1-7, where 4 is average)
                2. Dominant Jungian archetype (choose from: hero, caregiver, explorer, rebel, lover, creator, jester, sage, magician, ruler, innocent, everyman)
                3. Secondary archetype
                4. Empathy style (rate cognitive, affective, compassionate each 1-10)
                5. Current life stressors (identify from: work, finances, health, relationships, family, academic, social, self_image, future, loneliness)
                6. Key psychological insights

                Return ONLY valid JSON, no markdown, no explanation:
                {
                  "extraversion": <1-7>,
                  "agreeableness": <1-7>,
                  "conscientiousness": <1-7>,
                  "emotionalStability": <1-7>,
                  "openness": <1-7>,
                  "primaryArchetype": "<archetype>",
                  "secondaryArchetype": "<archetype>",
                  "cognitiveEmpathy": <1-10>,
                  "affectiveEmpathy": <1-10>,
                  "compassionateEmpathy": <1-10>,
                  "detectedStressors": ["stressor1", "stressor2"],
                  "insights": "<2-3 sentence professional psychological summary>"
                }
                """
                .formatted(qaPairs.toString());

        try {
            String response = geminiService.callGeminiWithRotation(prompt);
            String cleanJson = geminiService.cleanJsonResponse(response);
            log.info("Profile analysis: {}", cleanJson);

            Map<String, Object> result = objectMapper.readValue(
                    cleanJson,
                    new TypeReference<Map<String, Object>>() {
                    });

            return AnalyzedProfile.builder()
                    .extraversion(getInt(result, "extraversion", 4))
                    .agreeableness(getInt(result, "agreeableness", 4))
                    .conscientiousness(getInt(result, "conscientiousness", 4))
                    .emotionalStability(getInt(result, "emotionalStability", 4))
                    .openness(getInt(result, "openness", 4))
                    .primaryArchetype(getString(result, "primaryArchetype", "sage"))
                    .secondaryArchetype(getString(result, "secondaryArchetype", "explorer"))
                    .cognitiveEmpathy(getInt(result, "cognitiveEmpathy", 5))
                    .affectiveEmpathy(getInt(result, "affectiveEmpathy", 5))
                    .compassionateEmpathy(getInt(result, "compassionateEmpathy", 5))
                    .detectedStressors(getStringList(result, "detectedStressors"))
                    .insights(getString(result, "insights", "Analysis complete."))
                    .build();

        } catch (Exception e) {
            log.error("Failed to analyze responses: {}", e.getMessage(), e);
            return getDefaultProfile();
        }
    }

    // Helper methods
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
                        "Describe a moment when you felt truly alive and engaged. What were you doing?"),
                new AssessmentQuestion(2, "How do you typically react when facing unexpected challenges or setbacks?"),
                new AssessmentQuestion(3, "What qualities do you most admire in others, and why?"),
                new AssessmentQuestion(4, "Describe your ideal way to spend a free day with no obligations."),
                new AssessmentQuestion(5, "When you disagree with someone close to you, how do you usually handle it?"),
                new AssessmentQuestion(6, "What recurring worries or thoughts occupy your mind most often?"),
                new AssessmentQuestion(7, "How would your closest friend describe your personality in three words?"),
                new AssessmentQuestion(8,
                        "Describe a decision you made that went against others' expectations. How did it feel?"),
                new AssessmentQuestion(9,
                        "When someone you care about is going through difficulty, what's your instinct?"),
                new AssessmentQuestion(10,
                        "What do you believe is your greatest personal strength and your biggest challenge?"));
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
}
