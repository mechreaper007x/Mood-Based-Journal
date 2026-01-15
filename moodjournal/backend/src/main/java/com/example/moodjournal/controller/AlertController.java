package com.example.moodjournal.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.model.Alert;
import com.example.moodjournal.service.AlertService;
import com.example.moodjournal.service.UserService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserService userService;

    // Helper to get User ID
    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Not authenticated");
        }
        if (userDetails instanceof com.example.moodjournal.model.User user) {
            return user.getId();
        }
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userDetails.getUsername()))
                .getId();
    }

    /**
     * Get all alerts for the current user.
     */
    @GetMapping
    public ResponseEntity<?> getAlerts(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(alertService.getAlerts(getUserId(userDetails)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching alerts: " + e.getMessage()));
        }
    }

    /**
     * Get unread alerts only.
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Alert>> getUnreadAlerts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alertService.getUnreadAlerts(getUserId(userDetails)));
    }

    /**
     * Get unread count for badge display.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            long count = alertService.getUnreadCount(getUserId(userDetails));
            return ResponseEntity.ok(Map.of("count", count));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching unread count: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("count", 0L)); // Return 0 on error to prevent UI issues
        }
    }

    /**
     * Mark a specific alert as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        alertService.markAsRead(id, getUserId(userDetails));
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all alerts as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        alertService.markAllAsRead(getUserId(userDetails));
        return ResponseEntity.ok().build();
    }
}
