package com.example.moodjournal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.moodjournal.model.AssessmentSession;
import com.example.moodjournal.model.User;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {

    List<AssessmentSession> findByUserOrderByCompletedAtDesc(User user);

    List<AssessmentSession> findByUserIdOrderByCompletedAtDesc(java.util.UUID userId);

    java.util.Optional<AssessmentSession> findTopByUserIdOrderByCompletedAtDesc(java.util.UUID userId);

    /**
     * Get last 5 sessions for OCEAN drift detection.
     * Used by OceanDriftDetector to calculate historical personality baseline.
     */
    List<AssessmentSession> findTop5ByUserIdOrderByCompletedAtDesc(java.util.UUID userId);
}
