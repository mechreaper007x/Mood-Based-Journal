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
    private final com.example.moodjournal.service.AISecurityService aiSecurityService;
    private static final Logger log = LoggerFactory.getLogger(JournalEntryController.class);

    public JournalEntryController(JournalEntryService service, UserService userService,
            com.example.moodjournal.service.AISecurityService aiSecurityService) {
        this.service = service;
        this.userService = userService;
        this.aiSecurityService = aiSecurityService;
    }

    private java.util.UUID getUserIdFromUserDetails(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found")).getId();
    }

    
    @CrossOrigin
    @PostMapping
    public ResponseEntity<?> createEntry(@Valid @RequestBody CreateJournalEntryRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            java.util.UUID userId = getUserIdFromUserDetails(userDetails);

            
            
            try {
                String securedContent = aiSecurityService.securePrompt(req.getContent());
                req.setContent(securedContent); 
            } catch (SecurityException se) {
                log.warn("[SECURITY] Prompt injection BLOCKED at controller: {}", se.getMessage());
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Your entry contains content that appears to be unsafe. Please revise and try again."));
            }

            JournalEntry entry = new JournalEntry();
            entry.setTitle(req.getTitle());
            entry.setContent(req.getContent());
            
            if (req.getMood() != null && !req.getMood().isBlank()) {
                try {
                    entry.setMood(Mood.valueOf(req.getMood().toUpperCase())); 
                } catch (IllegalArgumentException e) {
                    
                }
            }
            if (req.getVisibility() != null && !req.getVisibility().isBlank()) {
                entry.setVisibility(Visibility.valueOf(req.getVisibility().toUpperCase()));
            } else {
                entry.setVisibility(Visibility.PRIVATE);
            }

            
            if (req.getAnalysisEmotion() != null && !req.getAnalysisEmotion().isBlank()) {
                entry.setAnalysisEmotion(req.getAnalysisEmotion());
            }
            if (req.getAnalysisConfidence() != null) {
                entry.setAnalysisConfidence(req.getAnalysisConfidence());
            }
            if (req.getAnalysisIntensity() != null) {
                entry.setAnalysisIntensity(req.getAnalysisIntensity());
            }

            
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

            JournalEntry created = service.create(userId, entry); 
                                                                  
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
        java.util.UUID userId = getUserIdFromUserDetails(userDetails);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<JournalEntry>> myEntries(@AuthenticationPrincipal UserDetails userDetails) {
        java.util.UUID userId = getUserIdFromUserDetails(userDetails);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @GetMapping("/community")
    public ResponseEntity<List<JournalEntry>> community(@RequestParam(required = false) String mood) {
        return ResponseEntity.ok(service.getPublicEntries(mood));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntry> get(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        java.util.UUID userId = getUserIdFromUserDetails(userDetails);
        return service.getById(id, userId).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateJournalEntryRequest updated,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            java.util.UUID userId = getUserIdFromUserDetails(userDetails);

            
            if (updated.getContent() != null) {
                try {
                    String securedContent = aiSecurityService.securePrompt(updated.getContent());
                    updated.setContent(securedContent);
                } catch (SecurityException se) {
                    log.warn("[SECURITY] Prompt injection BLOCKED at update controller: {}", se.getMessage());
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Your entry contains content that appears to be unsafe. Please revise and try again."));
                }
            }

            log.info("Update request for id={} payload={}", id, updated);
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
        java.util.UUID userId = getUserIdFromUserDetails(userDetails);
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<List<com.example.moodjournal.dto.MoodCount>> getStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        java.util.UUID userId = getUserIdFromUserDetails(userDetails);
        return ResponseEntity.ok(service.getMoodStatistics(userId));
    }

    @CrossOrigin
    @PostMapping("/{id}/reanalyze")
    public ResponseEntity<?> reanalyze(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            java.util.UUID userId = getUserIdFromUserDetails(userDetails);
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