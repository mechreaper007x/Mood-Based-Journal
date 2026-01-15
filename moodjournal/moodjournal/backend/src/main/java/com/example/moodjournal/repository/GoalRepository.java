package com.example.moodjournal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moodjournal.model.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(Long userId);

    long countByUserIdAndIsCompletedFalse(Long userId);
}
