package com.example.moodjournal.controller;

import java.util.List;
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

import com.example.moodjournal.dto.GoalRequest;
import com.example.moodjournal.model.Goal;
import com.example.moodjournal.model.User;
import com.example.moodjournal.service.GoalService;
import com.example.moodjournal.service.UserService;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    private GoalService goalService;

    @Autowired
    private UserService userService;

    private Long getUserId(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getId();
        }
        return userService.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userDetails.getUsername()));
    }

    /**
     * Get all goals for the current user.
     */
    @GetMapping
    public ResponseEntity<List<Goal>> getGoals(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getGoals(getUserId(userDetails)));
    }

    /**
     * Get only active (incomplete) goals.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Goal>> getActiveGoals(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(goalService.getActiveGoals(getUserId(userDetails)));
    }

    /**
     * Create a new goal.
     */
    @PostMapping
    public ResponseEntity<Goal> createGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.createGoal(getUserId(userDetails), request));
    }

    /**
     * Update a goal's progress or complete it.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(id, getUserId(userDetails), request));
    }

    /**
     * Delete a goal.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        goalService.deleteGoal(id, getUserId(userDetails));
        return ResponseEntity.ok().build();
    }
}