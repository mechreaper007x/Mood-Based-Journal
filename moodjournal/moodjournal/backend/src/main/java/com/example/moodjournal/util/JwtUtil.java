package com.example.moodjournal.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey key;
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; 
    private static final String TOKEN_ISSUER = "moodjournal-api";

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    


    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    


    public String generateToken(String username, Map<String, Object> claims) {
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(TOKEN_ISSUER)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        final String issuer = extractClaim(token, Claims::getIssuer);
        final Date issuedAt = extractClaim(token, Claims::getIssuedAt);
        boolean issuedAtValid = issuedAt == null || !issuedAt.after(new Date(System.currentTimeMillis() + 60_000));
        return extractedUsername.equals(username)
                && !isTokenExpired(token)
                && TOKEN_ISSUER.equals(issuer)
                && issuedAtValid;
    }

    



    public long getTimeToExpiry(String token) {
        try {
            Date expiration = extractExpiration(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, ttl);
        } catch (Exception e) {
            return 0;
        }
    }

    






    public boolean shouldRotate(String token, int rotationMinutes) {
        try {
            Claims claims = extractAllClaims(token);
            Long issuedAtMs = claims.get("iat_ms", Long.class);

            if (issuedAtMs == null) {
                
                Date issuedAt = claims.getIssuedAt();
                if (issuedAt == null)
                    return false;
                issuedAtMs = issuedAt.getTime();
            }

            long ageMinutes = (System.currentTimeMillis() - issuedAtMs) / (60 * 1000);
            return ageMinutes >= rotationMinutes;
        } catch (Exception e) {
            return false;
        }
    }
}
