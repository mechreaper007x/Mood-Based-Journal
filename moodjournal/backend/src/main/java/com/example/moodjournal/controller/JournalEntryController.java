package com.example.moodjournal.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.dto.CreateJournalEntryRequest;
import com.example.moodjournal.dto.UpdateJournalEntryRequest;
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.Mood;
import com.example.moodjournal.model.Visibility;
import com.example.moodjournal.service.JournalEntryService;
import com.example.moodjournal.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/journal")
public class JournalEntryController {
    private final JournalEntryService service;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(JournalEntryController.class);

    public JournalEntryController(JournalEntryService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found")).getId();
    }

    // POST /journal - create using request body only (user may be omitted)
    @CrossOrigin
    @PostMapping
    public ResponseEntity<?> createEntry(@Valid @RequestBody CreateJournalEntryRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            JournalEntry entry = new JournalEntry();
            entry.setTitle(req.getTitle());
            entry.setContent(req.getContent());
            // Set mood from request if it exists and is not empty
            if (req.getMood() != null && !req.getMood().isBlank()) {
                try {
                    entry.setMood(Mood.valueOf(req.getMood().toUpperCase())); // Convert string to Mood enum
                } catch (IllegalArgumentException e) {
                    // If mood is invalid, it will be null, and the service will auto-detect it
                }
            }
            if (req.getVisibility() != null && !req.getVisibility().isBlank()) {
                entry.setVisibility(Visibility.valueOf(req.getVisibility().toUpperCase()));
            } else {
                entry.setVisibility(Visibility.PRIVATE);
            }

            // Copy optional AI analysis metadata into the entry (if provided)
            if (req.getAnalysisEmotion() != null && !req.getAnalysisEmotion().isBlank()) {
                entry.setAnalysisEmotion(req.getAnalysisEmotion());
            }
            if (req.getAnalysisConfidence() != null) {
                entry.setAnalysisConfidence(req.getAnalysisConfidence());
            }
            if (req.getAnalysisIntensity() != null) {
                entry.setAnalysisIntensity(req.getAnalysisIntensity());
            }

            // Copy structured context fields
            if (req.getContextTags() != null) {
                entry.setContextTags(req.getContextTags());
            }
            if (req.getStressLevel() != null) {
                entry.setStressLevel(req.getStressLevel());
            }
            if (req.getEnergyLevel() != null) {
                entry.setEnergyLevel(req.getEnergyLevel());
            }
            if (req.getSleepQuality() != null) {
                entry.setSleepQuality(req.getSleepQuality());
            }
            if (req.getTriggerDescription() != null && !req.getTriggerDescription().isBlank()) {
                entry.setTriggerDescription(req.getTriggerDescription());
            }

            JournalEntry created = service.create(userId, entry); // Assuming this method exists and handles mood
                                                                  // detection
            URI location = URI.create(String.format("/journal/%d", created.getId()));
            return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating journal entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create entry: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<JournalEntry>> getAllEntries(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<JournalEntry>> myEntries(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @GetMapping("/community")
    public ResponseEntity<List<JournalEntry>> community(@RequestParam(required = false) String mood) {
        return ResponseEntity.ok(service.getPublicEntries(mood));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntry> get(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        return service.getById(id, userId).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateJournalEntryRequest updated,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            log.info("Update request for id={} payload={{}}", id, updated);
            JournalEntry result = service.update(id, userId, updated);
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            log.info("JournalEntry not found: id={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Journal entry not found"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating JournalEntry id={}", id, e);
            String msg = e.getMessage() == null ? "Internal server error" : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        }
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<List<com.example.moodjournal.dto.MoodCount>> getStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        return ResponseEntity.ok(service.getMoodStatistics(userId));
    }

    @CrossOrigin
    @PostMapping("/{id}/reanalyze")
    public ResponseEntity<?> reanalyze(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            JournalEntry updated = service.reanalyzeEntry(id, userId);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Entry not found"));
        } catch (Exception e) {
            log.error("Failed to reanalyze entry {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to analyze: " + e.getMessage()));
        }
    }

}