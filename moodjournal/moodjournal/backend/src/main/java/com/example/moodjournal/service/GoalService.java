package com.example.moodjournal.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moodjournal.dto.GoalRequest;
import com.example.moodjournal.model.Goal;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.GoalRepository;
import com.example.moodjournal.repository.UserRepository;

@Service
public class GoalService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Goal> getGoals(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Goal> getActiveGoals(Long userId) {
        return goalRepository.findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Goal createGoal(Long userId, GoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Goal goal = Goal.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(parseCategory(request.getCategory()))
                .targetDate(parseDate(request.getTargetDate()))
                .progress(0)
                .isCompleted(false)
                .build();

        return goalRepository.save(goal);
    }

    @Transactional
    public Goal updateGoal(Long goalId, Long userId, GoalRequest request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new NoSuchElementException("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new NoSuchElementException("Goal not found");
        }

        if (request.getTitle() != null) {
            goal.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            goal.setDescription(request.getDescription());
        }
        if (request.getProgress() != null) {
            goal.setProgress(request.getProgress());
        }
        if (request.getIsCompleted() != null) {
            boolean completed = request.getIsCompleted();
            goal.setIsCompleted(completed);
            if (completed) {
                goal.setProgress(100);
                goal.setCompletedAt(Instant.now());
            }
        }

        return goalRepository.save(goal);
    }

    @Transactional
    public void deleteGoal(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new NoSuchElementException("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new NoSuchElementException("Goal not found");
        }

        goalRepository.delete(goal);
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
