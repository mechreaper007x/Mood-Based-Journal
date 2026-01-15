package com.example.moodjournal.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.service.AnalyticsService;
import com.example.moodjournal.service.UserService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserService userService;

    // Helper to get User ID
    // Helper to get User ID
    private Long getUserId(UserDetails userDetails) {
        if (userDetails instanceof com.example.moodjournal.model.User user) {
            return user.getId();
        }
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userDetails.getUsername()))
                .getId();
    }

    /**
     * Get mood distribution over time (day by day)
     * Query param: range = week | month | year
     */
    @GetMapping("/mood-trend")
    public ResponseEntity<List<Map<String, Object>>> getMoodTrend(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "week") String range) {

        Instant since = switch (range.toLowerCase()) {
            case "month" -> Instant.now().minus(30, ChronoUnit.DAYS);
            case "year" -> Instant.now().minus(365, ChronoUnit.DAYS);
            default -> Instant.now().minus(7, ChronoUnit.DAYS);
        };

        return ResponseEntity.ok(analyticsService.getMoodTrend(getUserId(userDetails), since));
    }

    /**
     * Get emotional trajectory (improvement/decline over time)
     */
    @GetMapping("/trajectory")
    public ResponseEntity<Map<String, Object>> getTrajectory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getEmotionalTrajectory(getUserId(userDetails)));
    }

    /**
     * Get cognitive distortion frequency
     */
    /**
     * Get cognitive distortion frequency
     */
    @GetMapping("/distortion-frequency")
    public ResponseEntity<Map<String, Integer>> getDistortionFrequency(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getDistortionFrequency(getUserId(userDetails)));
    }

    /**
     * Get risk score history
     */
    @GetMapping("/risk-history")
    public ResponseEntity<List<Map<String, Object>>> getRiskHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "week") String range) {

        Instant since = switch (range.toLowerCase()) {
            case "month" -> Instant.now().minus(30, ChronoUnit.DAYS);
            case "year" -> Instant.now().minus(365, ChronoUnit.DAYS);
            default -> Instant.now().minus(7, ChronoUnit.DAYS);
        };

        return ResponseEntity.ok(analyticsService.getRiskHistory(getUserId(userDetails), since));
    }

    /**
     * Get overall stats summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getSummary(getUserId(userDetails)));
    }
}
