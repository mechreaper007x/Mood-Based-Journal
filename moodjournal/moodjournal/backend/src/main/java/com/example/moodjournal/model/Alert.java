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

import jakarta.persistence.UniqueConstraint;




@Entity
@Table(name = "alert", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "type", "triggerEntryId" })
})
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

    
    private Long triggerEntryId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum AlertType {
        HIGH_RISK, 
        DECLINING_TRAJECTORY, 
        CONSISTENT_DISTORTION, 
        CRISIS_KEYWORDS 
    }
}
