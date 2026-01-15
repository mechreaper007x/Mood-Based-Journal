package com.example.moodjournal.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.model.Visibility;

// This interface defines the repository for JournalEntry entities.
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByUserId(Long userId);

    List<JournalEntry> findByVisibility(Visibility visibility);

    List<JournalEntry> findByMoodAndVisibility(Mood mood, Visibility visibility);

    @Query("SELECT new com.example.moodjournal.dto.MoodCount(j.mood, COUNT(j)) FROM JournalEntry j WHERE j.user.id = :userId GROUP BY j.mood")
    List<com.example.moodjournal.dto.MoodCount> countMoodsByUserId(Long userId);

    // Get recent entries for emotional trajectory analysis
    List<JournalEntry> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    // Get top 10 for trajectory calculation
    List<JournalEntry> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    // Get entries after a certain date for analytics
    List<JournalEntry> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(Long userId, Instant since);
}