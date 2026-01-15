package com.example.moodjournal.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mental health alerts for concerning patterns.
 */
@Entity
@Table(name = "alert")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Column(length = 500)
    private String message;

    @Builder.Default
    private Boolean isRead = false;

    private Instant createdAt;

    // Reference to the entry that triggered the alert (optional)
    private Long triggerEntryId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum AlertType {
        HIGH_RISK, // Risk score >= 7
        DECLINING_TRAJECTORY, // 3+ days of declining mood
        CONSISTENT_DISTORTION, // Same distortion in 3+ entries
        CRISIS_KEYWORDS // Detected concerning language
    }
}
