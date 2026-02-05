package com.example.moodjournal.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(User user) {
        // Race condition fix (V2): Removed "check-then-act" logic.
        // We rely on the Database Unique Constraint and the below
        // DataIntegrityViolationException catch block.

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // V11 FIX: Explicit Pre-Check
        // We check existence before save. Note: A small race window exists here but is
        // better
        // than brittle exception parsing. For 100% strictness, we'd need a specific DB
        // exception handler,
        // but this "Lock-Free" approach is standard for Spring.
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already taken");
        }

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Fallback for race condition collisions
            log.warn("Data integrity violation during registration (Race Condition): {}", e.getMessage());
            throw new RuntimeException("Registration failed: User already exists.");
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
