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

        // 1. DETERMINISTIC SAFETY CHECK (The "Red Line")
        // We do not trust the AI with life-or-death classification.
        boolean hasCrisisKeywords = checkCrisisKeywords(entry.getContent());
        if (hasCrisisKeywords) {
            log.warn("!!! CRISIS KEYWORDS DETECTED for userId={} !!!", userId);
        }

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

            // OVERRIDE: If keywords were found, enforce minimum risk score
            if (hasCrisisKeywords && (result.getRiskScore() == null || result.getRiskScore() < 9)) {
                log.warn(">>> Overriding AI Risk Score ({} -> 9) due to keyword detection.", result.getRiskScore());
                result.setRiskScore(9);
                result.setNarrativeInsight(
                        result.getNarrativeInsight() + " [Safety Alert: Crisis resources triggered.]");
            }

            log.info(">>> Parsed result: distortions={}, risk={}, trajectory={}",
                    result.getCognitiveDistortions(), result.getRiskScore(), result.getEmotionalTrajectory());
            return result;
        } catch (Exception e) {
            log.error(">>> Profile-aware analysis FAILED: {}", e.getMessage(), e);
            EntryAnalysisResult fallback = getDefaultResult();
            if (hasCrisisKeywords) {
                fallback.setRiskScore(9);
                fallback.setNarrativeInsight(
                        "We noticed you might be going through a difficult moment. Please reach out for help.");
            }
            return fallback;
        }
    }

    private static final List<String> CRISIS_KEYWORDS = List.of(
            "suicide", "kill myself", "want to die", "end it all", "hurt myself");

    private boolean checkCrisisKeywords(String content) {
        if (content == null)
            return false;
        String lower = content.toLowerCase();
        return CRISIS_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Build a comprehensive analysis prompt with profile context.
     */
    private String buildAnalysisPrompt(UserProfileDTO profile, Long userId, JournalEntry entry) {
        StringBuilder prompt = new StringBuilder();

        // 1. Role Definition
        prompt.append("You are an advanced psychological analysis engine using the Ensemble Risk Engine model.\n");
        prompt.append("Your task is to analyze the following journal entry for emotional nuance and latent risk.\n\n");

        // 2. Profile Context (with Empathy Profile)
        if (profile != null) {
            prompt.append("=== USER PROFILE ===\n");
            prompt.append(String.format("Big Five Traits: O=%d, C=%d, E=%d, A=%d, N=%d\n",
                    profile.getOpenness() != null ? profile.getOpenness() : 4,
                    profile.getConscientiousness() != null ? profile.getConscientiousness() : 4,
                    profile.getExtraversion() != null ? profile.getExtraversion() : 4,
                    profile.getAgreeableness() != null ? profile.getAgreeableness() : 4,
                    profile.getEmotionalStability() != null ? profile.getEmotionalStability() : 4));
            prompt.append(String.format("Archetype: %s (secondary: %s)\n",
                    profile.getPrimaryArchetype() != null ? profile.getPrimaryArchetype() : "unknown",
                    profile.getSecondaryArchetype() != null ? profile.getSecondaryArchetype() : "unknown"));

            // Empathy Profile (The good stuff you wanted)
            prompt.append("Empathy Profile:\n");
            prompt.append(String.format("  - Cognitive Empathy: %d/10 (Understanding others' minds)\n",
                    profile.getCognitiveEmpathy() != null ? profile.getCognitiveEmpathy() : 5));
            prompt.append(String.format("  - Affective Empathy: %d/10 (Feeling others' emotions)\n",
                    profile.getAffectiveEmpathy() != null ? profile.getAffectiveEmpathy() : 5));
            prompt.append(String.format("  - Compassionate Empathy: %d/10 (Urge to help)\n",
                    profile.getCompassionateEmpathy() != null ? profile.getCompassionateEmpathy() : 5));

            if (profile.getCurrentStressors() != null && !profile.getCurrentStressors().isEmpty()) {
                prompt.append(String.format("Current Stressors: %s\n", profile.getCurrentStressors()));
            }
        }

        // 3. Add recent emotional trajectory
        prompt.append(buildRecentHistoryContext(userId));

        // 4. Add current entry details
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

        // 5. MERGED Output Format (Old Working Fields + New ISEAR/GoEmotions/VAD)
        prompt.append(
                """
                        === ANALYSIS INSTRUCTIONS ===
                        Analyze this entry considering the person's profile and provide ALL of the following:

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

                        --- NEW: SCIENTIFIC EXTENSIONS ---
                        8. PRIMARY EMOTION (ISEAR): Pick EXACTLY ONE from [JOY, FEAR, ANGER, SADNESS, DISGUST, SHAME, GUILT]
                        9. NUANCE TAGS (GoEmotions): Pick top 1-3 specific tags (e.g., 'admiration', 'annoyance', 'caring', 'disappointment', 'grief', 'nervousness', 'relief', 'remorse')
                        10. VAD SCORES (EmoBank):
                           - valence (0.0 to 1.0) -> Positivity/Negativity (0=Misery, 1=Ecstasy)
                           - arousal (0.0 to 1.0) -> Intensity/Energy (0=Sleepy, 1=Panic/Excitement)
                           - dominance (0.0 to 1.0) -> Control/Agency (0=Helpless, 1=In Control)

                        Return ONLY valid JSON, no markdown:
                        {
                          "dominantEmotion": "string",
                          "emotionBreakdown": {"anger": 0-100, "happiness": 0-100, "sadness": 0-100, "anxiety": 0-100, "calmness": 0-100},
                          "cognitiveDistortions": ["distortion1", "distortion2"],
                          "emotionalTrajectory": "improving|declining|stable",
                          "riskScore": 1-10,
                          "personalizedSuggestions": ["suggestion1", "suggestion2"],
                          "narrativeInsight": "2-3 sentence analysis",
                          "primaryEmotion": "JOY|FEAR|ANGER|SADNESS|DISGUST|SHAME|GUILT",
                          "nuanceTags": ["pride", "relief"],
                          "vadScores": { "valence": 0.9, "arousal": 0.6, "dominance": 0.8 }
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

            // Parse VAD Scores (new field)
            EntryAnalysisResult.VADScores vad = new EntryAnalysisResult.VADScores(0.5, 0.5, 0.5);
            if (result.get("vadScores") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> vadMap = (Map<String, Object>) result.get("vadScores");
                vad = EntryAnalysisResult.VADScores.builder()
                        .valence(getDouble(vadMap, "valence", 0.5))
                        .arousal(getDouble(vadMap, "arousal", 0.5))
                        .dominance(getDouble(vadMap, "dominance", 0.5))
                        .build();
            }

            // Get AI's risk score (but we can validate/override with VAD later)
            int aiRiskScore = getInt(result, "riskScore", 3);

            // Calculate VAD-based risk as backup/validation
            double vadCalculatedRisk = (1.0 - vad.getValence()) * vad.getArousal() * 10.0;
            int vadRiskScore = Math.max(1, Math.min(10, (int) Math.round(vadCalculatedRisk)));

            // Ensemble: Use higher of AI risk or VAD risk (safety first)
            int finalRiskScore = Math.max(aiRiskScore, vadRiskScore);

            return EntryAnalysisResult.builder()
                    // Scientific fields from new prompt
                    .primaryEmotion(getString(result, "primaryEmotion",
                            getString(result, "dominantEmotion", "NEUTRAL")))
                    .nuanceTags(getStringList(result, "nuanceTags"))
                    .vadScores(vad)

                    // Legacy/compatible fields
                    .emotionBreakdown(parseEmotionBreakdown(result))
                    .cognitiveDistortions(getStringList(result, "cognitiveDistortions"))
                    .emotionalTrajectory(getString(result, "emotionalTrajectory", "stable"))
                    .riskScore(finalRiskScore)
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

    private Double getDouble(Map<String, Object> map, String key, double defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number)
            return ((Number) val).doubleValue();
        return defaultVal;
    }

    // Legacy: Map new "PRIMARY_EMOTION" to old "dominantEmotion" (lowercase)
    private String convertPrimaryToDominant(String primary) {
        return primary != null ? primary.toLowerCase() : "neutral";
    }

    // Legacy: Generate fake breakdown map for frontend compatibility
    private Map<String, Integer> generateLegacyBreakdown(String primary, EntryAnalysisResult.VADScores vad) {
        Map<String, Integer> map = new HashMap<>();
        // Simple logic: Give 70% to primary, distribute rest
        String key = primary != null ? primary.toLowerCase() : "neutral";
        map.put(key, 70);
        map.put("other", 30);
        return map;
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
                .primaryEmotion("neutral")
                .emotionBreakdown(defaultBreakdown)
                .cognitiveDistortions(new ArrayList<>())
                .emotionalTrajectory("stable")
                .riskScore(2)
                .personalizedSuggestions(List.of("Continue journaling regularly", "Practice mindfulness"))
                .narrativeInsight("Analysis could not be completed. Please try again.")
                .build();
    }
}
