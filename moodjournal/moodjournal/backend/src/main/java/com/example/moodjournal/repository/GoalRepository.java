package com.example.moodjournal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moodjournal.model.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(java.util.UUID userId);

    List<Goal> findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(java.util.UUID userId);

    List<Goal> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(java.util.UUID userId);

    long countByUserIdAndIsCompletedFalse(java.util.UUID userId);
}
