package com.example.moodjournal.repository;

import com.example.moodjournal.model.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    // Fetch recent events to analyze clusters of attacks
    List<SecurityEvent> findTop100ByOrderByTimestampDesc();

    // Find events by violation type (e.g., fetch all "LAYER_2" blocks)
    List<SecurityEvent> findByViolationTypeOrderByTimestampDesc(String violationType);
}
