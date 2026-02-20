package com.example.moodjournal.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;











@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftAnalysis {

    
    private List<DriftWarning> warnings;

    
    private boolean driftDetected;

    
    private String validityStatus; 

    


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriftWarning {
        
        private String trait;

        
        private double historicalAverage;

        
        private double currentScore;

        
        private double driftMagnitude;

        
        private String severity;
    }

    


    public static DriftAnalysis noHistory() {
        return DriftAnalysis.builder()
                .driftDetected(false)
                .validityStatus("VALID")
                .warnings(List.of())
                .build();
    }
}
