package com.example.moodjournal.model;

import java.time.Instant;
import java.time.LocalDate;

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
 * Mental health goals for users to track their progress.
 */
@Entity
@Table(name = "goal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GoalCategory category = GoalCategory.GENERAL;

    private LocalDate targetDate;

    @Builder.Default
    private Integer progress = 0; // 0-100

    @Builder.Default
    private Boolean isCompleted = false;

    private Instant createdAt;
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum GoalCategory {
        JOURNALING, // e.g., "Write 3 entries this week"
        MOOD, // e.g., "Have more happy days"
        SLEEP, // e.g., "Improve sleep quality"
        STRESS, // e.g., "Reduce average stress level"
        MINDFULNESS, // e.g., "Practice daily gratitude"
        GENERAL // Custom goals
    }
}
