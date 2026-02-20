package com.example.moodjournal.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;





@Entity
@Table(name = "cached_question_set")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedQuestionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionsJson; 

    private Integer usageCount; 

    private Instant createdAt;

    @jakarta.persistence.Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.usageCount = 0;
    }

    public void incrementUsage() {
        this.usageCount++;
    }
}
