package com.example.moodjournal.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moodjournal.model.Alert;
import com.example.moodjournal.model.Alert.AlertType;
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.AlertRepository;
import com.example.moodjournal.repository.JournalEntryRepository;
import com.example.moodjournal.repository.UserRepository;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Check if alert should be generated based on entry analysis.
     * Called after journal entry analysis is complete.
     */
    @Transactional
    public void checkAndGenerateAlerts(Long userId, JournalEntry entry) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return;

        // Check for high risk score
        if (entry.getRiskScore() != null && entry.getRiskScore() >= 7) {
            generateAlert(user, AlertType.HIGH_RISK,
                    String.format(
                            "Your latest entry has a high mental health concern score (%d/10). Consider reaching out to someone you trust or a mental health professional.",
                            entry.getRiskScore()),
                    entry.getId());
            log.info("Generated HIGH_RISK alert for user {} with score {}", userId, entry.getRiskScore());
        }

        // Check for declining trajectory
        if ("declining".equalsIgnoreCase(entry.getEmotionalTrajectory())) {
            // Check if this is 3rd consecutive declining entry
            List<JournalEntry> recent = journalEntryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
            int decliningCount = 0;
            for (JournalEntry e : recent) {
                if ("declining".equalsIgnoreCase(e.getEmotionalTrajectory())) {
                    decliningCount++;
                } else {
                    break;
                }
            }

            if (decliningCount >= 3) {
                generateAlert(user, AlertType.DECLINING_TRAJECTORY,
                        "Your emotional trajectory has been declining for " + decliningCount
                                + " consecutive entries. It might be helpful to talk to someone about how you're feeling.",
                        entry.getId());
                log.info("Generated DECLINING_TRAJECTORY alert for user {}", userId);
            }
        }
    }

    private void generateAlert(User user, AlertType type, String message, Long entryId) {
        Alert alert = Alert.builder()
                .user(user)
                .type(type)
                .message(message)
                .triggerEntryId(entryId)
                .isRead(false)
                .build();
        alertRepository.save(alert);
    }

    public List<Alert> getAlerts(Long userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Alert> getUnreadAlerts(Long userId) {
        return alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return alertRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long alertId, Long userId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            if (alert.getUser().getId().equals(userId)) {
                alert.setIsRead(true);
                alertRepository.save(alert);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Alert> unread = alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(a -> a.setIsRead(true));
        alertRepository.saveAll(unread);
    }
}
