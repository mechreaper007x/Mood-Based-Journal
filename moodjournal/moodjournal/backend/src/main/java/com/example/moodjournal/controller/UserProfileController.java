package com.example.moodjournal.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.dto.UserProfileDTO;
import com.example.moodjournal.service.UserProfileService;
import com.example.moodjournal.service.UserService;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserService userService;

    


    @GetMapping
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                log.warn("getProfile called with null userDetails");
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            java.util.UUID userId = getUserId(userDetails);
            log.debug("Fetching profile for userId: {}", userId);
            return userProfileService.getProfileByUserId(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error loading profile: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Error loading profile: " + e.getMessage()));
        }
    }

    


    @GetMapping("/complete")
    public ResponseEntity<?> isProfileComplete(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated", "isComplete", false));
            }
            java.util.UUID userId = getUserId(userDetails);
            boolean complete = userProfileService.isProfileComplete(userId);
            return ResponseEntity.ok(Map.of("isComplete", complete));
        } catch (Exception e) {
            log.error("Error checking profile complete: {}", e.getMessage());
            
            return ResponseEntity.ok(Map.of("isComplete", false));
        }
    }

    


    @PostMapping
    public ResponseEntity<UserProfileDTO> saveProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfileDTO profileDTO) {
        java.util.UUID userId = getUserId(userDetails);
        UserProfileDTO saved = userProfileService.saveProfile(userId, profileDTO);
        return ResponseEntity.ok(saved);
    }

    


    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> markComplete(@AuthenticationPrincipal UserDetails userDetails) {
        java.util.UUID userId = getUserId(userDetails);
        userProfileService.markProfileComplete(userId);
        return ResponseEntity.ok(Map.of("message", "Profile marked as complete"));
    }

    
    private java.util.UUID getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            log.error("getUserId called with null userDetails");
            throw new RuntimeException("Not authenticated - userDetails is null");
        }

        if (userDetails instanceof com.example.moodjournal.model.User user) {
            log.debug("User found directly from UserDetails: id={}", user.getId());
            return user.getId();
        }

        String email = userDetails.getUsername();
        log.debug("Looking up user by email: {}", email);
        return userService.findByEmail(email)
                .map(user -> {
                    log.debug("User found by email lookup: id={}", user.getId());
                    return user.getId();
                })
                .orElseThrow(() -> {
                    log.error("User not found in database for email: {}", email);
                    return new RuntimeException("User not found: " + email);
                });
    }
}
