package com.example.moodjournal.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.moodjournal.dto.EntryAnalysisResult;
import com.example.moodjournal.dto.UserProfileDTO;
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.repository.JournalEntryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for profile-aware psychological analysis of journal entries.
 * Uses user's psychological profile to provide personalized insights,
 * detect cognitive distortions, and generate tailored suggestions.
 */
@Service
public class PsychologicalAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PsychologicalAnalysisService.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyze a journal entry with full profile context.
     */
    public EntryAnalysisResult analyzeWithProfile(Long userId, JournalEntry entry) {
        log.info(">>> Starting profile-aware analysis for userId={}", userId);

        // Get user's profile
        Optional<UserProfileDTO> profileOpt = userProfileService.getProfileByUserId(userId);
        log.info(">>> Profile found: {}", profileOpt.isPresent());

        // Build the prompt
        String prompt = buildAnalysisPrompt(profileOpt.orElse(null), userId, entry);
        log.info(">>> Prompt length: {} chars", prompt.length());

        try {
            log.info(">>> Calling Gemini API...");
            String response = geminiService.callGeminiWithRotation(prompt);
            log.info(">>> Raw Gemini response length: {}", response != null ? response.length() : 0);

            String cleanJson = geminiService.cleanJsonResponse(response);
            log.info(">>> Profile-aware analysis response: {}", cleanJson);

            EntryAnalysisResult result = parseAnalysisResult(cleanJson);
            log.info(">>> Parsed result: distortions={}, risk={}, trajectory={}",
                    result.getCognitiveDistortions(), result.getRiskScore(), result.getEmotionalTrajectory());
            return result;
        } catch (Exception e) {
            log.error(">>> Profile-aware analysis FAILED: {}", e.getMessage(), e);
            return getDefaultResult();
        }
    }

    /**
     * Build a comprehensive analysis prompt with profile context.
     */
    private String buildAnalysisPrompt(UserProfileDTO profile, Long userId, JournalEntry entry) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are a clinical psychologist providing a personalized analysis of a journal entry.
                Your role is to identify emotional patterns, detect cognitive distortions, and provide
                tailored insights based on the person's psychological profile.

                """);

        // Add profile context if available
        if (profile != null) {
            prompt.append("=== USER PSYCHOLOGICAL PROFILE ===\n");
            prompt.append(String.format("Big Five Personality:\n"));
            prompt.append(String.format("  - Extraversion: %d/7\n",
                    profile.getExtraversion() != null ? profile.getExtraversion() : 4));
            prompt.append(String.format("  - Agreeableness: %d/7\n",
                    profile.getAgreeableness() != null ? profile.getAgreeableness() : 4));
            prompt.append(String.format("  - Conscientiousness: %d/7\n",
                    profile.getConscientiousness() != null ? profile.getConscientiousness() : 4));
            prompt.append(String.format("  - Emotional Stability: %d/7\n",
                    profile.getEmotionalStability() != null ? profile.getEmotionalStability() : 4));
            prompt.append(
                    String.format("  - Openness: %d/7\n\n", profile.getOpenness() != null ? profile.getOpenness() : 4));

            prompt.append(String.format("Psychological Archetype: %s (secondary: %s)\n\n",
                    profile.getPrimaryArchetype() != null ? profile.getPrimaryArchetype() : "unknown",
                    profile.getSecondaryArchetype() != null ? profile.getSecondaryArchetype() : "unknown"));

            prompt.append(String.format("Empathy Profile:\n"));
            prompt.append(String.format("  - Cognitive: %d/10\n",
                    profile.getCognitiveEmpathy() != null ? profile.getCognitiveEmpathy() : 5));
            prompt.append(String.format("  - Affective: %d/10\n",
                    profile.getAffectiveEmpathy() != null ? profile.getAffectiveEmpathy() : 5));
            prompt.append(String.format("  - Compassionate: %d/10\n\n",
                    profile.getCompassionateEmpathy() != null ? profile.getCompassionateEmpathy() : 5));

            if (profile.getCurrentStressors() != null && !profile.getCurrentStressors().isEmpty()) {
                prompt.append(String.format("Known Stressors: %s\n\n", profile.getCurrentStressors()));
            }
        } else {
            prompt.append("(No profile data available - provide general analysis)\n\n");
        }

        // Add recent emotional trajectory
        prompt.append(buildRecentHistoryContext(userId));

        // Add current entry details
        prompt.append("=== CURRENT JOURNAL ENTRY ===\n");
        prompt.append(String.format("Title: %s\n", entry.getTitle()));

        if (entry.getContextTags() != null && !entry.getContextTags().isEmpty()) {
            prompt.append(String.format("Context Tags: %s\n", String.join(", ", entry.getContextTags())));
        }
        if (entry.getStressLevel() != null) {
            prompt.append(String.format("Current Stress Level: %d/10\n", entry.getStressLevel()));
        }
        if (entry.getEnergyLevel() != null) {
            prompt.append(String.format("Current Energy Level: %d/10\n", entry.getEnergyLevel()));
        }
        if (entry.getSleepQuality() != null) {
            prompt.append(String.format("Last Night's Sleep: %d/5\n", entry.getSleepQuality()));
        }
        if (entry.getTriggerDescription() != null && !entry.getTriggerDescription().isBlank()) {
            prompt.append(String.format("Trigger: %s\n", entry.getTriggerDescription()));
        }
        prompt.append(String.format("\nContent:\n%s\n\n", entry.getContent()));

        // Analysis instructions
        prompt.append(
                """
                        === ANALYSIS INSTRUCTIONS ===
                        Analyze this entry considering the person's profile and provide:

                        1. EMOTION BREAKDOWN: Percentages for anger, happiness, sadness, anxiety, calmness (must sum to 100)
                        2. DOMINANT EMOTION: The primary emotion expressed
                        3. COGNITIVE DISTORTIONS: Detect any of these patterns:
                           - all-or-nothing: Black/white thinking ("always", "never", "everyone")
                           - catastrophizing: Expecting worst outcomes ("disaster", "ruined")
                           - mind-reading: Assuming others' thoughts ("they think I'm...")
                           - emotional-reasoning: Feelings as facts ("I feel stupid so I am")
                           - should-statements: Rigid expectations ("I should", "must")
                           - overgeneralization: One event = always ("this always happens")
                           - personalization: Self-blame for external events
                        4. EMOTIONAL TRAJECTORY: Based on recent entries, is the person improving, declining, or stable?
                        5. RISK SCORE: Mental health concern level 1-10 (1=healthy, 10=crisis)
                        6. PERSONALIZED SUGGESTIONS: 2-3 specific suggestions based on their archetype and profile
                        7. NARRATIVE INSIGHT: 2-3 sentence professional analysis

                        Return ONLY valid JSON, no markdown:
                        {
                          "dominantEmotion": "string",
                          "emotionBreakdown": {"anger": 0-100, "happiness": 0-100, "sadness": 0-100, "anxiety": 0-100, "calmness": 0-100},
                          "cognitiveDistortions": ["distortion1", "distortion2"],
                          "emotionalTrajectory": "improving|declining|stable",
                          "riskScore": 1-10,
                          "personalizedSuggestions": ["suggestion1", "suggestion2"],
                          "narrativeInsight": "2-3 sentence analysis"
                        }
                        """);

        return prompt.toString();
    }

    /**
     * Build context from recent journal entries for trajectory analysis.
     */
    private String buildRecentHistoryContext(Long userId) {
        StringBuilder context = new StringBuilder();
        context.append("=== RECENT EMOTIONAL HISTORY ===\n");

        try {
            List<JournalEntry> recentEntries = journalEntryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);

            if (recentEntries.isEmpty()) {
                context.append("(No previous entries - this is their first entry)\n\n");
            } else {
                for (int i = 0; i < Math.min(3, recentEntries.size()); i++) {
                    JournalEntry e = recentEntries.get(i);
                    context.append(String.format("- %s: Mood=%s, Emotion=%s\n",
                            e.getCreatedAt() != null ? e.getCreatedAt().toString().substring(0, 10) : "unknown",
                            e.getMood() != null ? e.getMood().name() : "unknown",
                            e.getAnalysisEmotion() != null ? e.getAnalysisEmotion() : "unknown"));
                }
                context.append("\n");
            }
        } catch (Exception e) {
            context.append("(Unable to fetch history)\n\n");
        }

        return context.toString();
    }

    /**
     * Parse the JSON response into EntryAnalysisResult.
     */
    private EntryAnalysisResult parseAnalysisResult(String json) {
        try {
            Map<String, Object> result = objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {
                    });

            return EntryAnalysisResult.builder()
                    .dominantEmotion(getString(result, "dominantEmotion", "neutral"))
                    .emotionBreakdown(parseEmotionBreakdown(result))
                    .cognitiveDistortions(getStringList(result, "cognitiveDistortions"))
                    .emotionalTrajectory(getString(result, "emotionalTrajectory", "stable"))
                    .riskScore(getInt(result, "riskScore", 3))
                    .personalizedSuggestions(getStringList(result, "personalizedSuggestions"))
                    .narrativeInsight(getString(result, "narrativeInsight", "Analysis complete."))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse analysis result: {}", e.getMessage());
            return getDefaultResult();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> parseEmotionBreakdown(Map<String, Object> result) {
        Map<String, Integer> breakdown = new HashMap<>();
        Object emotions = result.get("emotionBreakdown");
        if (emotions instanceof Map) {
            Map<String, Object> emotionMap = (Map<String, Object>) emotions;
            for (String key : emotionMap.keySet()) {
                Object val = emotionMap.get(key);
                if (val instanceof Number) {
                    breakdown.put(key, ((Number) val).intValue());
                }
            }
        }
        return breakdown;
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private Integer getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number)
            return ((Number) val).intValue();
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List)
            return (List<String>) val;
        return new ArrayList<>();
    }

    private EntryAnalysisResult getDefaultResult() {
        Map<String, Integer> defaultBreakdown = new HashMap<>();
        defaultBreakdown.put("calmness", 60);
        defaultBreakdown.put("happiness", 20);
        defaultBreakdown.put("sadness", 10);
        defaultBreakdown.put("anxiety", 10);
        defaultBreakdown.put("anger", 0);

        return EntryAnalysisResult.builder()
                .dominantEmotion("neutral")
                .emotionBreakdown(defaultBreakdown)
                .cognitiveDistortions(new ArrayList<>())
                .emotionalTrajectory("stable")
                .riskScore(2)
                .personalizedSuggestions(List.of("Continue journaling regularly", "Practice mindfulness"))
                .narrativeInsight("Analysis could not be completed. Please try again.")
                .build();
    }
}
