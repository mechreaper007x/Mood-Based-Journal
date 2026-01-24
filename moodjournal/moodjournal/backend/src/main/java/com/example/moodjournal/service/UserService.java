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
        // Check for duplicate username
        if (user.getUsername() != null && userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already taken");
        }

        // Check for duplicate email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // This catches race conditions where another request registered the same user
            log.warn("Data integrity violation during registration: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("USERNAME")) {
                throw new RuntimeException("Username is already taken");
            } else if (e.getMessage() != null && e.getMessage().contains("EMAIL")) {
                throw new RuntimeException("Email is already taken");
            }
            throw new RuntimeException("Registration failed. Please try again.");
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
