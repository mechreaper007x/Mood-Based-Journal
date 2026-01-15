package com.example.moodjournal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moodjournal.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Alert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
