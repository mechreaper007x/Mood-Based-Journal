package com.example.moodjournal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moodjournal.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserIdOrderByCreatedAtDesc(java.util.UUID userId);

    List<Alert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(java.util.UUID userId);

    long countByUserIdAndIsReadFalse(java.util.UUID userId);
}
