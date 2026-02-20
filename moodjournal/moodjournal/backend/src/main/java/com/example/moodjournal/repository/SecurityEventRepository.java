package com.example.moodjournal.repository;

import com.example.moodjournal.model.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    
    List<SecurityEvent> findTop100ByOrderByTimestampDesc();

    
    List<SecurityEvent> findTop500ByOrderByTimestampDesc();

    
    List<SecurityEvent> findByViolationTypeOrderByTimestampDesc(String violationType);

    List<SecurityEvent> findByTimestampAfterOrderByTimestampDesc(LocalDateTime since);
}
