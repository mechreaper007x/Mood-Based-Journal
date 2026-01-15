package com.example.moodjournal.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.model.Goal;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.GoalRepository;
import com.example.moodjournal.service.UserService;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UserService userService;

    private User getUser(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user;
        }
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userDetails.getUsername()));
    }

    /**
     * Get all goals for the current user.
     */
    /**
     * Get all goals for the current user.
     */
    @GetMapping
    public ResponseEntity<List<Goal>> getGoals(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalRepository.findByUserIdOrderByCreatedAtDesc(getUser(userDetails).getId()));
    }

    /**
     * Get only active (incomplete) goals.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Goal>> getActiveGoals(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .ok(goalRepository.findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(getUser(userDetails).getId()));
    }

    /**
     * Create a new goal.
     */
    @PostMapping
    public ResponseEntity<Goal> createGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        User user = getUser(userDetails);

        Goal goal = Goal.builder()
                .user(user)
                .title((String) body.get("title"))
                .description((String) body.get("description"))
                .category(parseCategory((String) body.get("category")))
                .targetDate(parseDate((String) body.get("targetDate")))
                .progress(0)
                .isCompleted(false)
                .build();

        return ResponseEntity.ok(goalRepository.save(goal));
    }

    /**
     * Update a goal's progress or complete it.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        User user = getUser(userDetails);

        Goal goal = goalRepository.findById(id).orElse(null);
        if (goal == null || !goal.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        if (body.containsKey("title")) {
            goal.setTitle((String) body.get("title"));
        }
        if (body.containsKey("description")) {
            goal.setDescription((String) body.get("description"));
        }
        if (body.containsKey("progress")) {
            goal.setProgress((Integer) body.get("progress"));
        }
        if (body.containsKey("isCompleted")) {
            boolean completed = (Boolean) body.get("isCompleted");
            goal.setIsCompleted(completed);
            if (completed) {
                goal.setProgress(100);
                goal.setCompletedAt(Instant.now());
            }
        }

        return ResponseEntity.ok(goalRepository.save(goal));
    }

    /**
     * Delete a goal.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        Goal goal = goalRepository.findById(id).orElse(null);
        if (goal == null || !goal.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        goalRepository.delete(goal);
        return ResponseEntity.ok().build();
    }

    private Goal.GoalCategory parseCategory(String cat) {
        if (cat == null)
            return Goal.GoalCategory.GENERAL;
        try {
            return Goal.GoalCategory.valueOf(cat.toUpperCase());
        } catch (Exception e) {
            return Goal.GoalCategory.GENERAL;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isEmpty())
            return null;
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}
