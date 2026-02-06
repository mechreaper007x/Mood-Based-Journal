package com.example.moodjournal.repository;

import com.example.moodjournal.model.SecurityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityRuleRepository extends JpaRepository<SecurityRule, UUID> {

    // Fetch only active rules to load into the Bouncer
    List<SecurityRule> findByIsActiveTrue();

    // Fetch shadow rules for testing
    List<SecurityRule> findByIsShadowModeTrue();
}
