package com.example.moodjournal.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import com.example.moodjournal.model.PasswordResetToken;
import com.example.moodjournal.model.User;
import com.example.moodjournal.repository.PasswordResetTokenRepository;
import com.example.moodjournal.repository.UserRepository;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a password reset token for the given email and sends a reset email.
     * Returns true if the email exists (for internal use), but the API should
     * always
     * return success to prevent user enumeration.
     */
    @Transactional
    public boolean createPasswordResetToken(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return false;
        }

        User user = userOptional.get();

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        log.info("Created password reset token for user: {}", email);

        // Send email (or log to console in dev mode)
        emailService.sendPasswordResetEmail(email, token);

        return true;
    }

    /**
     * Validates a password reset token.
     * Returns the associated user if the token is valid.
     */
    public Optional<User> validateToken(String token) {
        // V12 FIX: Constant-time lookup simulation
        // We always perform the same number of DB operations and checks regardless of
        // validity
        // to prevent timing attacks.

        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);

        // Always simulate validation check even if token is missing
        boolean isValidChain = true;

        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();
            if (!resetToken.isValid()) {
                isValidChain = false;
                // Log without revealing too much context
                log.warn("Invalid token attempt encountered");
            }
        } else {
            isValidChain = false;
            // Fake computation to mimic check time
            PasswordResetToken.builder().expiryDate(java.time.LocalDateTime.now()).build().isValid();
            log.warn("Invalid token check completed");
        }

        if (!isValidChain) {
            return Optional.empty();
        }

        return Optional.of(tokenOptional.get().getUser());
    }

    /**
     * Resets the user's password using the provided token.
     * Returns true if successful.
     */
    /**
     * Resets the user's password using the provided token.
     * Returns true if successful.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);

        if (tokenOptional.isEmpty()) {
            log.warn("Password reset attempted with invalid token");
            return false;
        }

        PasswordResetToken resetToken = tokenOptional.get();

        if (!resetToken.isValid()) {
            log.warn("Password reset attempted with expired or used token");
            return false;
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
        return true;
    }

    /**
     * Cleans up expired tokens. Can be scheduled to run periodically.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(java.time.LocalDateTime.now());
        log.info("Cleaned up expired password reset tokens");
    }
}
