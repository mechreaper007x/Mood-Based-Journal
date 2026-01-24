package com.example.moodjournal.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.moodjournal.dto.ForgotPasswordRequest;
import com.example.moodjournal.dto.RegisterRequest;
import com.example.moodjournal.dto.ResetPasswordRequest;
import com.example.moodjournal.model.User;
import com.example.moodjournal.service.PasswordResetService;
import com.example.moodjournal.service.UserService;
import com.example.moodjournal.util.JwtUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    public UserController(
            UserService userService,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtUtil jwtUtil,
            PasswordResetService passwordResetService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.passwordResetService = passwordResetService;
    }

    @CrossOrigin
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Convert DTO to User entity
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .age(request.getAge())
                    .build();

            User registeredUser = userService.register(user);

            final UserDetails userDetails = userDetailsService.loadUserByUsername(registeredUser.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails.getUsername());

            registeredUser.setPassword(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", jwt, "user", registeredUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @CrossOrigin
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        log.info("--- LOGIN METHOD CALLED ---");
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credentials.get("email"), credentials.get("password")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
        }

        final String email = credentials.get("email");
        final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        final String jwt = jwtUtil.generateToken(userDetails.getUsername());

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        user.setPassword(null);
        return ResponseEntity.ok(Map.of("token", jwt, "user", user));
    }

    /**
     * Initiates the password reset flow.
     * Always returns success to prevent user enumeration attacks.
     */
    @CrossOrigin
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("--- FORGOT PASSWORD REQUESTED for: {} ---", request.getEmail());

        // Process the request (will log the reset link in dev mode)
        passwordResetService.createPasswordResetToken(request.getEmail());

        // Always return success to prevent user enumeration
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists with this email, you will receive a password reset link.",
                "success", true));
    }

    /**
     * Validates a password reset token.
     */
    @CrossOrigin
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        log.info("--- VALIDATING RESET TOKEN ---");

        Optional<User> user = passwordResetService.validateToken(token);

        if (user.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "email", user.get().getEmail()));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "Invalid or expired reset token"));
        }
    }

    /**
     * Resets the user's password using the provided token.
     */
    @CrossOrigin
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("--- RESET PASSWORD REQUESTED ---");

        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(Map.of(
                    "message", "Password has been reset successfully. You can now log in with your new password.",
                    "success", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid or expired reset token. Please request a new password reset.",
                    "success", false));
        }
    }
}
