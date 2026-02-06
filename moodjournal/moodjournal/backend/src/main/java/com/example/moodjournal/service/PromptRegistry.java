package com.example.moodjournal.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Prompt Registry with versioning and rollback capability.
 * 
 * Features:
 * - Version history for each prompt
 * - Instant rollback to any previous version
 * - Audit logging for prompt changes
 * - Active/Deprecated version tracking
 * 
 * IMPORTANT: In production, consider persisting to database
 * for durability across restarts.
 */
@Service
public class PromptRegistry {

    private static final Logger log = LoggerFactory.getLogger(PromptRegistry.class);

    /** All prompt versions keyed by promptId */
    private final Map<String, List<PromptVersion>> versionHistory = new ConcurrentHashMap<>();

    /** Current active version index for each prompt */
    private final Map<String, Integer> activeVersions = new ConcurrentHashMap<>();

    /**
     * Register a new prompt or add a new version.
     * 
     * @param promptId Unique identifier (e.g., "assessment.question.generator")
     * @param template Prompt template string
     * @param reason   Reason for change (for audit)
     * @return New version number
     */
    public int registerVersion(String promptId, String template, String reason) {
        List<PromptVersion> history = versionHistory.computeIfAbsent(promptId, k -> new ArrayList<>());

        int version = history.size() + 1;
        PromptVersion newVersion = PromptVersion.builder()
                .version(version)
                .template(template)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .deprecated(false)
                .build();

        history.add(newVersion);
        activeVersions.put(promptId, version - 1); // 0-indexed

        log.info("Registered prompt version {}.v{}: {}", promptId, version, reason);
        return version;
    }

    /**
     * Get the active prompt template.
     */
    public String getActivePrompt(String promptId) {
        List<PromptVersion> history = versionHistory.get(promptId);
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("Prompt not found: " + promptId);
        }

        int activeIndex = activeVersions.getOrDefault(promptId, history.size() - 1);
        return history.get(activeIndex).getTemplate();
    }

    /**
     * Rollback to a specific version.
     * 
     * @param promptId Prompt identifier
     * @param version  Version to rollback to (1-indexed)
     * @param reason   Reason for rollback
     */
    public void rollback(String promptId, int version, String reason) {
        List<PromptVersion> history = versionHistory.get(promptId);
        if (history == null || version < 1 || version > history.size()) {
            throw new IllegalArgumentException("Invalid rollback: prompt=" + promptId + ", version=" + version);
        }

        int previousActive = activeVersions.getOrDefault(promptId, history.size() - 1) + 1;
        activeVersions.put(promptId, version - 1);

        log.warn("ROLLBACK: {} from v{} to v{} - Reason: {}", promptId, previousActive, version, reason);

        // Mark newer versions as deprecated
        for (int i = version; i < history.size(); i++) {
            history.get(i).setDeprecated(true);
        }
    }

    /**
     * Get version history for a prompt.
     */
    public List<PromptVersion> getHistory(String promptId) {
        return versionHistory.getOrDefault(promptId, List.of());
    }

    /**
     * Get current active version number.
     */
    public int getActiveVersion(String promptId) {
        return activeVersions.getOrDefault(promptId, 0) + 1;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptVersion {
        private int version;
        private String template;
        private String reason;
        private LocalDateTime createdAt;
        private boolean deprecated;
    }
}
