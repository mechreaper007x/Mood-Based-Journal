package com.example.moodjournal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Crisis flag for PHQ-9 Item 9 detection.
 * Triggers when user indicates self-harm ideation.
 * 
 * Clinical Reference: PHQ-9 Item 9 asks:
 * "Thoughts that you would be better off dead or of hurting yourself"
 * ANY non-zero response requires immediate crisis intervention per APA
 * guidelines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrisisFlag {

    /** Whether crisis flag was triggered */
    private boolean triggered;

    /** Severity level: MODERATE (1), HIGH (2-3) */
    private String severity;

    /** Clinical reason for flag */
    private String reason;

    /** Recommended immediate action */
    private String immediateAction;

    /** Crisis hotline numbers to display */
    @Builder.Default
    private String crisisHotline = "988 (Suicide & Crisis Lifeline)";

    /** Whether to block assessment submission until acknowledged */
    @Builder.Default
    private boolean requiresAcknowledgment = false;

    /**
     * Static factory for non-triggered state.
     */
    public static CrisisFlag none() {
        return CrisisFlag.builder()
                .triggered(false)
                .build();
    }

    /**
     * Static factory for triggered state.
     */
    public static CrisisFlag triggered(int itemScore) {
        String severity = itemScore >= 2 ? "HIGH" : "MODERATE";
        return CrisisFlag.builder()
                .triggered(true)
                .severity(severity)
                .reason("PHQ-9 Item 9 indicates self-harm ideation (score=" + itemScore + ")")
                .immediateAction("Display crisis resources and suggest professional support")
                .requiresAcknowledgment(itemScore >= 2)
                .build();
    }
}
