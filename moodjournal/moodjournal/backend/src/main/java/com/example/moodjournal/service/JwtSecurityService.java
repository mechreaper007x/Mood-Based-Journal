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

    
    
    

    




    public String generateSecureToken(String email, HttpServletRequest request) {
        String fingerprint = createFingerprint(
                request.getHeader("User-Agent"),
                getClientIp(request),
                request.getHeader("Accept-Language"));

        String fingerprintHash = hashFingerprint(fingerprint);

        
        long tokenTtl = 10 * 60 * 60 * 1000L; 
        tokenStore.setWithExpiry(
                FINGERPRINT_PREFIX + email,
                fingerprintHash,
                tokenTtl);

        
        Map<String, Object> claims = new HashMap<>();
        claims.put("fp", fingerprintHash.substring(0, 16)); 
        claims.put("iat_ms", System.currentTimeMillis()); 

        String token = jwtUtil.generateToken(email, claims);

        
        tokenStore.setWithExpiry(
                USER_TOKENS_PREFIX + email,
                token.substring(token.length() - 32), 
                tokenTtl);

        log.info("Generated secure token for user: {} [fingerprint={}]",
                email, fingerprintHash.substring(0, 8) + "...");

        return token;
    }

    
    
    

    



    public boolean validateTokenFingerprint(String token, HttpServletRequest request) {
        if (!fingerprintEnabled) {
            return true; 
        }

        try {
            String email = jwtUtil.extractUsername(token);
            String storedHash = tokenStore.get(FINGERPRINT_PREFIX + email);

            if (storedHash == null) {
                log.warn("No stored fingerprint for user: {}", email);
                return false; 
            }

            String currentFingerprint = createFingerprint(
                    request.getHeader("User-Agent"),
                    getClientIp(request),
                    request.getHeader("Accept-Language"));
            String currentHash = hashFingerprint(currentFingerprint);

            
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

    public boolean validateTokenBinding(String token) {
        try {
            String email = jwtUtil.extractUsername(token);
            String storedTokenSuffix = tokenStore.get(USER_TOKENS_PREFIX + email);
            if (storedTokenSuffix == null) {
                log.warn("No active token binding for user: {}", email);
                return false;
            }

            String tokenSuffix = token.length() > 32 ? token.substring(token.length() - 32) : token;
            boolean suffixMatch = MessageDigest.isEqual(
                    storedTokenSuffix.getBytes(StandardCharsets.UTF_8),
                    tokenSuffix.getBytes(StandardCharsets.UTF_8));
            if (!suffixMatch) {
                log.warn("Token suffix mismatch for user: {}", email);
                return false;
            }

            String storedFingerprintHash = tokenStore.get(FINGERPRINT_PREFIX + email);
            if (storedFingerprintHash == null) {
                log.warn("No fingerprint binding for user: {}", email);
                return false;
            }

            String tokenFingerprintFragment = jwtUtil.extractClaim(token, claims -> claims.get("fp", String.class));
            if (tokenFingerprintFragment == null) {
                log.warn("Missing fingerprint claim for user: {}", email);
                return false;
            }

            String expectedFragment = storedFingerprintHash.substring(0, Math.min(16, storedFingerprintHash.length()));
            boolean fragmentMatch = MessageDigest.isEqual(
                    expectedFragment.getBytes(StandardCharsets.UTF_8),
                    tokenFingerprintFragment.getBytes(StandardCharsets.UTF_8));
            if (!fragmentMatch) {
                log.warn("Token fingerprint claim mismatch for user: {}", email);
            }

            return fragmentMatch;
        } catch (Exception e) {
            log.error("Error validating token binding", e);
            return false;
        }
    }

    
    
    

    


    public boolean shouldRotateToken(String token) {
        try {
            Long issuedAt = jwtUtil.extractClaim(token, claims -> claims.get("iat_ms", Long.class));
            if (issuedAt == null) {
                return false; 
            }

            long ageMinutes = (System.currentTimeMillis() - issuedAt) / (60 * 1000);
            return ageMinutes >= rotationMinutes;

        } catch (Exception e) {
            return false;
        }
    }

    


    public String rotateToken(String oldToken, HttpServletRequest request) {
        String email = jwtUtil.extractUsername(oldToken);

        
        long ttl = jwtUtil.getTimeToExpiry(oldToken);
        if (ttl > 0) {
            tokenStore.setWithExpiry(
                    BLACKLIST_PREFIX + oldToken,
                    "rotated",
                    ttl);
        }

        log.info("Token rotated for user: {}", email);

        
        return generateSecureToken(email, request);
    }

    
    
    

    


    public boolean isTokenBlacklisted(String token) {
        return tokenStore.exists(BLACKLIST_PREFIX + token);
    }

    


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

    


    public void revokeAllUserTokens(String email) {
        tokenStore.delete(FINGERPRINT_PREFIX + email);
        tokenStore.delete(USER_TOKENS_PREFIX + email);
        log.info("All tokens revoked for user: {}", email);
    }

    
    
    

    




    private String createFingerprint(String userAgent, String ip, String acceptLang) {
        return String.join("|",
                userAgent != null ? userAgent : "unknown",
                ip != null ? ip.substring(0, Math.min(ip.length(), 12)) : "unknown", 
                acceptLang != null ? acceptLang.split(",")[0] : "en" 
        );
    }

    


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
