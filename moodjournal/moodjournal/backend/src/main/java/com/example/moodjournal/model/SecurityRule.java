package com.example.moodjournal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_rules")
public class SecurityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String pattern; 

    @Column(name = "description")
    private String description; 

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "is_shadow_mode")
    private boolean isShadowMode; 

    @Column(name = "blocked_count")
    private int blockedCount;

    private LocalDateTime createdAt;
    private LocalDateTime lastTriggeredAt;

    public SecurityRule() {
        this.createdAt = LocalDateTime.now();
        this.blockedCount = 0;
    }

    public SecurityRule(String pattern, String description, boolean isActive, boolean isShadowMode) {
        this.id = UUID.randomUUID();
        this.pattern = pattern;
        this.description = description;
        this.isActive = isActive;
        this.isShadowMode = isShadowMode;
        this.createdAt = LocalDateTime.now();
        this.blockedCount = 0;
    }

    public void incrementBlockedCount() {
        this.blockedCount++;
        this.lastTriggeredAt = LocalDateTime.now();
    }

    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isShadowMode() {
        return isShadowMode;
    }

    public void setShadowMode(boolean shadowMode) {
        isShadowMode = shadowMode;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(int blockedCount) {
        this.blockedCount = blockedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }
}
