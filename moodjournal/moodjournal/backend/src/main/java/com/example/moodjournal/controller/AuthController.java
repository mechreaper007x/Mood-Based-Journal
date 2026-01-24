package com.example.moodjournal.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    public AuthController(
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

    @CrossOrigin
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.createPasswordResetToken(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists with this email, you will receive a password reset link.",
                "success", true));
    }

    @CrossOrigin
    @PostMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        var user = passwordResetService.validateToken(token);
        if (user.isPresent()) {
            return ResponseEntity.ok(Map.of("valid", true, "email", user.get().getEmail()));
        } else {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Invalid token"));
        }
    }

    @CrossOrigin
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid token"));
        }
    }
}
