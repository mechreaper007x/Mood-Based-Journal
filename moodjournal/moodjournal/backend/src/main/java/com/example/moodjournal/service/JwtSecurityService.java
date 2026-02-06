package com.example.moodjournal.service;

import com.example.moodjournal.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Multi-Layer JWT Security Service
 * 
 * Implements 4-layer defense:
 * 1. Token Fingerprinting (Anti-Theft) - Binds token to browser characteristics
 * 2. Token Blacklisting (Revocation) - Enables instant token invalidation
 * 3. Token Rotation (Anti-Replay) - Periodically issues fresh tokens
 * 4. Standard Validation - Signature and expiry checks
 */
@Service
public class JwtSecurityService {

    private static final Logger log = LoggerFactory.getLogger(JwtSecurityService.class);

    private static final String FINGERPRINT_PREFIX = "fp:";
    private static final String BLACKLIST_PREFIX = "bl:";
    private static final String USER_TOKENS_PREFIX = "ut:";

    private final TokenStore tokenStore;
    private final JwtUtil jwtUtil;

    @Value("${jwt.rotation.minutes:30}")
    private int rotationMinutes;

    @Value("${jwt.fingerprint.enabled:true}")
    private boolean fingerprintEnabled;

    public JwtSecurityService(TokenStore tokenStore, JwtUtil jwtUtil) {
        this.tokenStore = tokenStore;
        this.jwtUtil = jwtUtil;
    }

    // ============================================
    // TOKEN GENERATION WITH FINGERPRINTING
    // ============================================

    /**
     * Generate a secure token with browser fingerprint embedded.
     * The fingerprint helps detect if a token is being used from a different
     * device/browser.
     */
    public String generateSecureToken(String email, HttpServletRequest request) {
        String fingerprint = createFingerprint(
                request.getHeader("User-Agent"),
                getClientIp(request),
                request.getHeader("Accept-Language"));

        String fingerprintHash = hashFingerprint(fingerprint);

        // Store full fingerprint hash with token expiry (10 hours)
        long tokenTtl = 10 * 60 * 60 * 1000L; // 10 hours in ms
        tokenStore.setWithExpiry(
                FINGERPRINT_PREFIX + email,
                fingerprintHash,
                tokenTtl);

        // Embed partial fingerprint in JWT claims for stateless verification fallback
        Map<String, Object> claims = new HashMap<>();
        claims.put("fp", fingerprintHash.substring(0, 16)); // Partial for JWT size
        claims.put("iat_ms", System.currentTimeMillis()); // For rotation check

        String token = jwtUtil.generateToken(email, claims);

        // Track user's active token (for logout-all-devices)
        tokenStore.setWithExpiry(
                USER_TOKENS_PREFIX + email,
                token.substring(token.length() - 32), // Last 32 chars as identifier
                tokenTtl);

        log.info("Generated secure token for user: {} [fingerprint={}]",
                email, fingerprintHash.substring(0, 8) + "...");

        return token;
    }

    // ============================================
    // FINGERPRINT VALIDATION
    // ============================================

    /**
     * Validate that the request fingerprint matches the stored fingerprint.
     * Returns true if fingerprinting is disabled or if fingerprints match.
     */
    public boolean validateTokenFingerprint(String token, HttpServletRequest request) {
        if (!fingerprintEnabled) {
            return true; // Fingerprinting disabled
        }

        try {
            String email = jwtUtil.extractUsername(token);
            String storedHash = tokenStore.get(FINGERPRINT_PREFIX + email);

            if (storedHash == null) {
                log.warn("No stored fingerprint for user: {}", email);
                return false; // Token expired or fingerprint not found
            }

            String currentFingerprint = createFingerprint(
                    request.getHeader("User-Agent"),
                    getClientIp(request),
                    request.getHeader("Accept-Language"));
            String currentHash = hashFingerprint(currentFingerprint);

            // Constant-time comparison to prevent timing attacks
            boolean match = MessageDigest.isEqual(
                    storedHash.getBytes(StandardCharsets.UTF_8),
                    currentHash.getBytes(StandardCharsets.UTF_8));

            if (!match) {
                log.warn("Fingerprint mismatch for user: {} - possible token theft!", email);
            }

            return match;

        } catch (Exception e) {
            log.error("Error validating fingerprint", e);
            return false;
        }
    }

    // ============================================
    // TOKEN ROTATION (Anti-Replay)
    // ============================================

    /**
     * Check if token should be rotated based on age.
     */
    public boolean shouldRotateToken(String token) {
        try {
            Long issuedAt = jwtUtil.extractClaim(token, claims -> claims.get("iat_ms", Long.class));
            if (issuedAt == null) {
                return false; // Legacy token without iat_ms claim
            }

            long ageMinutes = (System.currentTimeMillis() - issuedAt) / (60 * 1000);
            return ageMinutes >= rotationMinutes;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Rotate token: blacklist old token and issue new one.
     */
    public String rotateToken(String oldToken, HttpServletRequest request) {
        String email = jwtUtil.extractUsername(oldToken);

        // Blacklist old token
        long ttl = jwtUtil.getTimeToExpiry(oldToken);
        if (ttl > 0) {
            tokenStore.setWithExpiry(
                    BLACKLIST_PREFIX + oldToken,
                    "rotated",
                    ttl);
        }

        log.info("Token rotated for user: {}", email);

        // Issue new token with fresh fingerprint
        return generateSecureToken(email, request);
    }

    // ============================================
    // TOKEN BLACKLISTING (Revocation)
    // ============================================

    /**
     * Check if token is blacklisted (revoked).
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenStore.exists(BLACKLIST_PREFIX + token);
    }

    /**
     * Revoke a specific token (e.g., on logout).
     */
    public void revokeToken(String token) {
        try {
            long ttl = jwtUtil.getTimeToExpiry(token);
            if (ttl > 0) {
                tokenStore.setWithExpiry(
                        BLACKLIST_PREFIX + token,
                        "revoked",
                        ttl);
                String email = jwtUtil.extractUsername(token);
                tokenStore.delete(FINGERPRINT_PREFIX + email);
                log.info("Token revoked for user: {}", email);
            }
        } catch (Exception e) {
            log.error("Error revoking token", e);
        }
    }

    /**
     * Revoke all tokens for a user (logout from all devices).
     */
    public void revokeAllUserTokens(String email) {
        tokenStore.delete(FINGERPRINT_PREFIX + email);
        tokenStore.delete(USER_TOKENS_PREFIX + email);
        log.info("All tokens revoked for user: {}", email);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Create browser fingerprint from request characteristics.
     * Uses partial IP to allow for minor network changes (e.g., mobile carrier
     * NAT).
     */
    private String createFingerprint(String userAgent, String ip, String acceptLang) {
        return String.join("|",
                userAgent != null ? userAgent : "unknown",
                ip != null ? ip.substring(0, Math.min(ip.length(), 12)) : "unknown", // Partial IP
                acceptLang != null ? acceptLang.split(",")[0] : "en" // Primary language only
        );
    }

    /**
     * Get client IP, handling proxies and load balancers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Hash fingerprint using SHA-256.
     */
    private String hashFingerprint(String fingerprint) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
