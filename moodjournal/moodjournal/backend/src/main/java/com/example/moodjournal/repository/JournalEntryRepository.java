package com.example.moodjournal.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.model.Visibility;


public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByUserId(java.util.UUID userId);

    List<JournalEntry> findByVisibility(Visibility visibility);

    List<JournalEntry> findByMoodAndVisibility(Mood mood, Visibility visibility);

    @Query("SELECT new com.example.moodjournal.dto.MoodCount(j.mood, COUNT(j)) FROM JournalEntry j WHERE j.user.id = :userId GROUP BY j.mood")
    List<com.example.moodjournal.dto.MoodCount> countMoodsByUserId(java.util.UUID userId);

    
    List<JournalEntry> findTop5ByUserIdOrderByCreatedAtDesc(java.util.UUID userId);

    
    List<JournalEntry> findTop10ByUserIdOrderByCreatedAtDesc(java.util.UUID userId);

    
    List<JournalEntry> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(java.util.UUID userId, Instant since);

  
  List<JournalEntry> findTop100ByOrderByCreatedAtDesc();

  
  List<JournalEntry> findTop500ByOrderByCreatedAtDesc();
}
