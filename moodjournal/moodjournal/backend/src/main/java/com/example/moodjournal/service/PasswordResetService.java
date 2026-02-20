package com.example.moodjournal.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
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

    





    @Transactional
    public boolean createPasswordResetToken(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return false;
        }

        User user = userOptional.get();

        
        tokenRepository.deleteByUser(user);

        
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        log.info("Created password reset token for user: {}", email);

        
        emailService.sendPasswordResetEmail(email, token);

        return true;
    }

    








    public Optional<User> validateToken(String token) {
        
        
        List<PasswordResetToken> allTokens = tokenRepository.findAllNonExpired();

        PasswordResetToken matchedToken = null;
        boolean foundMatch = false;

        
        if (token == null || token.isEmpty()) {
            log.warn("Empty token validation attempt");
            return Optional.empty();
        }

        byte[] inputBytes = token.getBytes(StandardCharsets.UTF_8);

        
        
        for (PasswordResetToken t : allTokens) {
            byte[] storedBytes = t.getToken().getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(inputBytes, storedBytes)) {
                matchedToken = t;
                foundMatch = true;
                
            }
        }

        
        boolean isValid = foundMatch && matchedToken != null && matchedToken.isValid();

        if (!isValid) {
            log.warn("Invalid token attempt (constant-time check completed)");
            return Optional.empty();
        }

        return Optional.of(matchedToken.getUser());
    }

    



    



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

        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
        return true;
    }

    


    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(java.time.LocalDateTime.now());
        log.info("Cleaned up expired password reset tokens");
    }
}
