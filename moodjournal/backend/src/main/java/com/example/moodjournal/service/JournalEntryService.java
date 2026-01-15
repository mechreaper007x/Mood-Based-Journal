package com.example.moodjournal.service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.moodjournal.dto.EntryAnalysisResult;
import com.example.moodjournal.dto.UpdateJournalEntryRequest;
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.model.User;
import com.example.moodjournal.model.Visibility;
import com.example.moodjournal.repository.JournalEntryRepository;
import com.example.moodjournal.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JournalEntryService {

  private static final Logger log = LoggerFactory.getLogger(JournalEntryService.class);

  private final JournalEntryRepository entryRepo;
  private final UserRepository userRepo;
  private final GeminiService geminiService;
  private final ObjectMapper objectMapper;
  private final PsychologicalAnalysisService psychAnalysisService;
  private final AlertService alertService;

  public JournalEntryService(
      JournalEntryRepository entryRepo,
      UserRepository userRepo,
      GeminiService geminiService,
      ObjectMapper objectMapper,
      PsychologicalAnalysisService psychAnalysisService,
      AlertService alertService) {
    this.entryRepo = entryRepo;
    this.userRepo = userRepo;
    this.geminiService = geminiService;
    this.objectMapper = objectMapper;
    this.psychAnalysisService = psychAnalysisService;
    this.alertService = alertService;
  }

  public JournalEntry create(Long userId, JournalEntry entry) {
    User user = userRepo
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
    entry.setUser(user);

    // Only analyze if mood is NOT already set (to prevent duplicate API calls)
    if (entry.getMood() == null) {
      log.info("No mood provided, running profile-aware analysis...");
      analyzeAndSetMood(userId, entry);
    } else {
      log.info("Mood already provided from frontend: {}, skipping analysis", entry.getMood());
    }

    JournalEntry saved = entryRepo.save(entry);

    // Check for alerts after analysis
    try {
      alertService.checkAndGenerateAlerts(userId, saved);
    } catch (Exception e) {
      log.error("Alert generation failed: {}", e.getMessage());
    }

    return saved;
  }

  private void analyzeAndSetMood(Long userId, JournalEntry entry) {
    log.info("=== Starting profile-aware analysis for entry ===");
    log.info("Content preview: {}", entry.getContent().substring(0, Math.min(100, entry.getContent().length())));

    try {
      // Use the new profile-aware analysis service
      EntryAnalysisResult result = psychAnalysisService.analyzeWithProfile(userId, entry);

      // Set emotion breakdown as detailed analysis
      if (result.getEmotionBreakdown() != null) {
        StringBuilder breakdown = new StringBuilder();
        result.getEmotionBreakdown().forEach((k, v) -> breakdown.append(k).append(": ").append(v).append("%, "));
        entry.setDetailedAnalysis(breakdown.toString().replaceAll(", $", ""));
      }

      // Set dominant emotion
      if (result.getDominantEmotion() != null) {
        entry.setAnalysisEmotion(result.getDominantEmotion());
        Mood mood = mapEmotionToMood(result.getDominantEmotion().toLowerCase());
        entry.setMood(mood);
        log.info("Dominant emotion: {} -> Mood: {}", result.getDominantEmotion(), mood);
      } else {
        entry.setMood(Mood.NEUTRAL);
      }

      // Set new profile-aware analysis fields
      if (result.getCognitiveDistortions() != null && !result.getCognitiveDistortions().isEmpty()) {
        entry.setCognitiveDistortions(String.join(",", result.getCognitiveDistortions()));
      }
      entry.setRiskScore(result.getRiskScore());
      entry.setEmotionalTrajectory(result.getEmotionalTrajectory());

      // Store suggestions as JSON
      if (result.getPersonalizedSuggestions() != null) {
        try {
          entry.setSuggestions(objectMapper.writeValueAsString(result.getPersonalizedSuggestions()));
        } catch (Exception e) {
          entry.setSuggestions(String.join("; ", result.getPersonalizedSuggestions()));
        }
      }

      // Set narrative insight as detailed analysis if present
      if (result.getNarrativeInsight() != null) {
        String current = entry.getDetailedAnalysis() != null ? entry.getDetailedAnalysis() + " | " : "";
        entry.setDetailedAnalysis(current + result.getNarrativeInsight());
      }

      log.info("Profile-aware analysis complete: distortions={}, risk={}, trajectory={}",
          result.getCognitiveDistortions(), result.getRiskScore(), result.getEmotionalTrajectory());

    } catch (Exception e) {
      log.error("Profile-aware analysis failed, falling back to basic: {}", e.getMessage(), e);
      fallbackBasicAnalysis(entry);
    }
  }

  /**
   * Fallback to basic Gemini analysis if profile-aware fails.
   */
  private void fallbackBasicAnalysis(JournalEntry entry) {
    try {
      String emotionJson = geminiService.analyzeEmotions(entry.getContent());
      Map<String, Object> analysis = objectMapper.readValue(
          emotionJson, new TypeReference<Map<String, Object>>() {
          });

      String dominantEmotion = getStringFromMap(analysis, "DominantEmotion", "dominantEmotion");
      if (dominantEmotion != null) {
        entry.setAnalysisEmotion(dominantEmotion);
        entry.setMood(mapEmotionToMood(dominantEmotion.toLowerCase()));
      } else {
        entry.setMood(Mood.NEUTRAL);
      }
    } catch (Exception e) {
      log.error("Fallback analysis also failed: {}", e.getMessage());
      entry.setMood(Mood.NEUTRAL);
      entry.setDetailedAnalysis("Analysis failed: " + e.getMessage());
    }
  }

  // Helper to parse "60%" or 60 -> int 60
  private int parsePercent(Object val) {
    if (val == null)
      return 0;
    if (val instanceof Number)
      return ((Number) val).intValue();
    if (val instanceof String) {
      String s = ((String) val).replace("%", "").trim();
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        // Try parsing as double and converting
        try {
          return (int) Double.parseDouble(s);
        } catch (NumberFormatException e2) {
          return 0;
        }
      }
    }
    return 0;
  }

  // Helper to find and parse percent with multiple possible key names
  private int parsePercentFromMap(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      Object val = map.get(key);
      if (val != null) {
        int result = parsePercent(val);
        log.debug("Found key '{}' with value '{}' -> {}", key, val, result);
        return result;
      }
    }
    log.warn("No value found for keys: {}", java.util.Arrays.toString(keys));
    return 0;
  }

  // Helper to find a string value with multiple possible key names
  private String getStringFromMap(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      Object val = map.get(key);
      if (val != null) {
        return val.toString();
      }
    }
    return null;
  }

  public Mood suggestMood(String content) {
    log.info("=== suggestMood called ===");
    try {
      // Use Gemini for mood suggestion
      String emotionJson = geminiService.analyzeEmotions(content);
      Map<String, Object> analysis = objectMapper.readValue(
          emotionJson,
          new TypeReference<Map<String, Object>>() {
          });

      // Get dominant emotion and map to mood
      String dominantEmotion = (String) analysis.get("DominantEmotion");
      if (dominantEmotion != null) {
        Mood mood = mapEmotionToMood(dominantEmotion.toLowerCase());
        log.info("Mapped emotion '{}' -> Mood: {}", dominantEmotion, mood);
        return mood;
      }
    } catch (Exception e) {
      log.error("Gemini Analysis Failed: {}", e.getMessage(), e);
    }
    log.warn("Falling back to NEUTRAL");
    return Mood.NEUTRAL;
  }

  private Mood mapEmotionToMood(String emotion) {
    // Map detailed emotions to Mood categories
    return switch (emotion) {
      case "happiness", "happy", "joy", "jubilation", "ecstasy", "elation", "bliss", "cheerfulness",
          "glee", "delight", "delighted", "pleased", "thrilled", "euphoria", "gratitude",
          "thankfulness", "satisfaction", "amusement", "enjoyment", "optimistic", "optimism" ->
        Mood.HAPPY;
      case "sadness", "sad", "sorrow", "grief", "melancholy", "dejection", "despair", "miserable",
          "gloomy", "unhappy", "hurt", "suffering", "anguish", "agony", "hopelessness",
          "lonely", "loneliness", "missing", "isolation", "miss", "lost", "empty",
          "tired", "exhausted", "fatigue", "drained", "weary", "burnout" ->
        Mood.SAD;
      case "fear", "afraid", "scared", "terrified", "horrified", "dread", "fright",
          "terror", "panicked", "apprehension", "nervous", "worried", "anxious",
          "stressed", "tension", "uneasiness", "insecurity", "overwhelmed", "pressure" ->
        Mood.ANXIOUS;
      case "anger", "rage", "fury", "wrath", "annoyed", "irritable", "frustrated",
          "frustration", "resentment", "outrage", "hate", "hatred", "contempt", "bitter" ->
        Mood.ANGRY;
      case "calm", "serenity", "relaxed", "peaceful", "tranquil", "comfortable",
          "patience", "acceptance", "tolerance", "carefree", "steady", "balanced" ->
        Mood.CALM;
      case "energetic", "energy", "determination", "determined", "motivated", "motivation",
          "driven", "focused", "power", "strong", "strength", "active", "dynamic", "vibrant", "fire" ->
        Mood.ENERGETIC;
      case "content", "contentment", "fulfilled", "fulfillment", "at_ease", "satisfied" ->
        Mood.CONTENT;
      case "excited", "excitement", "eager", "anticipation", "looking_forward", "hyped", "enthusiastic" ->
        Mood.EXCITED;
      default -> Mood.NEUTRAL;
    };
  }

  public List<com.example.moodjournal.dto.MoodCount> getMoodStatistics(Long userId) {
    return entryRepo.countMoodsByUserId(userId);
  }

  public JournalEntry reanalyzeEntry(Long id, Long userId) {
    return entryRepo.findById(id)
        .map(entry -> {
          if (!entry.getUser().getId().equals(userId)) {
            throw new NoSuchElementException("Entry not found");
          }
          // Re-run profile-aware analysis
          analyzeAndSetMood(userId, entry);
          return entryRepo.save(entry);
        })
        .orElseThrow(() -> new NoSuchElementException("Entry not found"));
  }

  public List<JournalEntry> getByUser(Long userId) {
    return entryRepo.findByUserId(userId);
  }

  public List<JournalEntry> getPublicEntries(String mood) {
    if (mood != null && !mood.isEmpty()) {
      try {
        Mood moodEnum = Mood.valueOf(mood.toUpperCase());
        return entryRepo.findByMoodAndVisibility(moodEnum, Visibility.PUBLIC_ANON);
      } catch (IllegalArgumentException e) {
        return List.of();
      }
    }
    return entryRepo.findByVisibility(Visibility.PUBLIC_ANON);
  }

  public Optional<JournalEntry> getById(Long id, Long userId) {
    return entryRepo
        .findById(id)
        .map(entry -> {
          if (!entry.getUser().getId().equals(userId)) {
            throw new NoSuchElementException("JournalEntry not found");
          }
          return entry;
        });
  }

  public JournalEntry update(
      Long id,
      Long userId,
      UpdateJournalEntryRequest updated) {
    return entryRepo
        .findById(id)
        .map(e -> {
          if (!e.getUser().getId().equals(userId)) {
            throw new NoSuchElementException("JournalEntry not found");
          }
          e.setTitle(updated.getTitle());
          e.setContent(updated.getContent());

          if (updated.getContent() != null && !updated.getContent().equals(e.getContent())) {
            Mood suggestedMood = suggestMood(updated.getContent());
            if (updated.getMood() == null || updated.getMood().isBlank() || e.getMood() == null) {
              e.setMood(suggestedMood);
            }
          }

          if (updated.getMood() != null && !updated.getMood().isBlank()) {
            try {
              e.setMood(Mood.valueOf(updated.getMood().toUpperCase()));
            } catch (IllegalArgumentException ex) {
            }
          }
          if (updated.getVisibility() != null && !updated.getVisibility().isBlank()) {
            try {
              e.setVisibility(Visibility.valueOf(updated.getVisibility().toUpperCase()));
            } catch (IllegalArgumentException ex) {
            }
          }
          return entryRepo.save(e);
        })
        .orElseThrow(() -> new NoSuchElementException("JournalEntry not found"));
  }

  public JournalEntry updateJournal(Long id, JournalEntry updatedEntry) {
    return entryRepo
        .findById(id)
        .map(e -> {
          e.setTitle(updatedEntry.getTitle());
          e.setContent(updatedEntry.getContent());
          e.setMood(updatedEntry.getMood());
          e.setVisibility(updatedEntry.getVisibility());
          return entryRepo.save(e);
        })
        .orElseThrow(() -> new NoSuchElementException("JournalEntry not found"));
  }

  public void delete(Long id, Long userId) {
    entryRepo
        .findById(id)
        .ifPresentOrElse(
            entry -> {
              if (!entry.getUser().getId().equals(userId)) {
                throw new NoSuchElementException("JournalEntry not found");
              }
              entryRepo.deleteById(id);
            },
            () -> {
              throw new NoSuchElementException("JournalEntry not found");
            });
  }
}