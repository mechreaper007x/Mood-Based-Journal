package com.example.moodjournal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;










@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrisisFlag {

    
    private boolean triggered;

    
    private String severity;

    
    private String reason;

    
    private String immediateAction;

    
    @Builder.Default
    private String crisisHotline = "988 (Suicide & Crisis Lifeline)";

    
    @Builder.Default
    private boolean requiresAcknowledgment = false;

    


    public static CrisisFlag none() {
        return CrisisFlag.builder()
                .triggered(false)
                .build();
    }

    


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
