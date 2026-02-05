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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public boolean verifyAndAuthenticate(String username, String jwt,
            org.springframework.security.core.userdetails.UserDetails userDetails,
            Object authenticationDetails, com.example.moodjournal.util.JwtUtil jwtUtil) {

        // 1. PESSIMISTIC LOCK: Ensure no other thread is disabling the user right now
        // This transaction holds the lock until the method returns
        Optional<User> dbUser = userRepository.findByEmailForUpdate(username);

        boolean isValid = false;
        if (dbUser.isPresent()) {
            User user = dbUser.get();
            if (!user.isEnabled()) {
                log.warn("Authentication failed: User {} is disabled (Atomic Check)", username);
            } else if (!user.isAccountNonLocked()) {
                log.warn("Authentication failed: User {} is locked (Atomic Check)", username);
            } else {
                isValid = true;
            }
        } else {
            log.warn("Authentication atomic check failed: User {} not found in DB", username);
        }

        // 2. Set Authentication while Lock is still held (Semantic Atomicity)
        if (isValid && jwtUtil.validateToken(jwt, userDetails.getUsername())) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            authToken.setDetails(authenticationDetails);

            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authToken);
            log.info("User authenticated atomically: {}", username);
            return true;
        }

        return false;
    }
}
