package com.example.moodjournal.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.repository.JournalEntryRepository;

@Service
public class AnalyticsService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    /**
     * Get mood counts per day for the given time range.
     */
    public List<Map<String, Object>> getMoodTrend(Long userId, Instant since) {
        List<JournalEntry> entries = journalEntryRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                userId, since);

        // Group by date
        Map<LocalDate, Map<Mood, Long>> byDate = entries.stream()
                .filter(e -> e.getMood() != null && e.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(JournalEntry::getMood, Collectors.counting())));

        List<Map<String, Object>> result = new ArrayList<>();
        byDate.forEach((date, moods) -> {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("moods", moods);
            dayData.put("total", moods.values().stream().mapToLong(Long::longValue).sum());
            result.add(dayData);
        });

        return result;
    }

    /**
     * Calculate emotional trajectory based on recent entries.
     */
    public Map<String, Object> getEmotionalTrajectory(Long userId) {
        List<JournalEntry> recent = journalEntryRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> result = new HashMap<>();

        if (recent.isEmpty()) {
            result.put("trajectory", "stable");
            result.put("confidence", 0);
            result.put("message", "Not enough data");
            return result;
        }

        // Calculate average positivity score (happy=2, calm=1, sad=-1, angry=-2, etc.)
        List<Integer> scores = recent.stream()
                .map(e -> moodToScore(e.getMood()))
                .collect(Collectors.toList());

        // Compare first half vs second half
        int halfSize = scores.size() / 2;
        if (halfSize == 0)
            halfSize = 1;

        double recentAvg = scores.subList(0, halfSize).stream()
                .mapToInt(Integer::intValue).average().orElse(0);
        double olderAvg = scores.subList(halfSize, scores.size()).stream()
                .mapToInt(Integer::intValue).average().orElse(0);

        String trajectory;
        if (recentAvg > olderAvg + 0.5) {
            trajectory = "improving";
        } else if (recentAvg < olderAvg - 0.5) {
            trajectory = "declining";
        } else {
            trajectory = "stable";
        }

        result.put("trajectory", trajectory);
        result.put("recentScore", Math.round(recentAvg * 10) / 10.0);
        result.put("olderScore", Math.round(olderAvg * 10) / 10.0);
        result.put("entriesAnalyzed", recent.size());

        return result;
    }

    private int moodToScore(Mood mood) {
        if (mood == null)
            return 0;
        return switch (mood) {
            case HAPPY -> 2;
            case JOYFUL -> 2;
            case EXCITED -> 2;
            case CALM -> 1;
            case CONTENT -> 1;
            case ENERGETIC -> 1;
            case PRODUCTIVE -> 1;
            case NEUTRAL -> 0;
            case ANXIOUS -> -1;
            case SAD -> -2;
            case ANGRY -> -2;
            default -> 0;
        };
    }

    /**
     * Get frequency of cognitive distortions.
     */
    public Map<String, Integer> getDistortionFrequency(Long userId) {
        List<JournalEntry> entries = journalEntryRepository.findByUserId(userId);

        Map<String, Integer> frequency = new HashMap<>();

        entries.stream()
                .filter(e -> e.getCognitiveDistortions() != null && !e.getCognitiveDistortions().isEmpty())
                .forEach(e -> {
                    for (String distortion : e.getCognitiveDistortions().split(",")) {
                        String cleaned = distortion.trim().toLowerCase();
                        if (!cleaned.isEmpty()) {
                            frequency.merge(cleaned, 1, Integer::sum);
                        }
                    }
                });

        return frequency;
    }

    /**
     * Get risk score history over time.
     */
    public List<Map<String, Object>> getRiskHistory(Long userId, Instant since) {
        List<JournalEntry> entries = journalEntryRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                userId, since);

        return entries.stream()
                .filter(e -> e.getRiskScore() != null && e.getCreatedAt() != null)
                .map(e -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", e.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
                    point.put("riskScore", e.getRiskScore());
                    point.put("mood", e.getMood() != null ? e.getMood().name() : "NEUTRAL");
                    return point;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get summary statistics.
     */
    public Map<String, Object> getSummary(Long userId) {
        List<JournalEntry> allEntries = journalEntryRepository.findByUserId(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEntries", allEntries.size());

        // Mood distribution
        Map<String, Long> moodDistribution = allEntries.stream()
                .filter(e -> e.getMood() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getMood().name(),
                        Collectors.counting()));
        summary.put("moodDistribution", moodDistribution);

        // Average risk score
        double avgRisk = allEntries.stream()
                .filter(e -> e.getRiskScore() != null)
                .mapToInt(JournalEntry::getRiskScore)
                .average()
                .orElse(0);
        summary.put("averageRiskScore", Math.round(avgRisk * 10) / 10.0);

        // Most common mood
        String mostCommonMood = moodDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NEUTRAL");
        summary.put("mostCommonMood", mostCommonMood);

        // Entries this week
        long thisWeek = allEntries.stream()
                .filter(e -> e.getCreatedAt() != null &&
                        e.getCreatedAt().isAfter(Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS)))
                .count();
        summary.put("entriesThisWeek", thisWeek);

        return summary;
    }
}
