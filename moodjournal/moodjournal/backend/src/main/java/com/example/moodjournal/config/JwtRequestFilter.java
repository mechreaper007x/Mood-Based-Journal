package com.example.moodjournal.config;

import com.example.moodjournal.service.JwtSecurityService;
import com.example.moodjournal.util.JwtUtil;
import com.example.moodjournal.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Multi-Layer JWT Request Filter
 * 
 * Implements 4-layer defense:
 * 1. BLACKLIST CHECK - Reject revoked tokens immediately
 * 2. FINGERPRINT VALIDATION - Detect stolen tokens
 * 3. STANDARD JWT VALIDATION - Signature + expiry + user status
 * 4. AUTO-ROTATION - Issue new token via X-New-Token header
 */
public class JwtRequestFilter extends OncePerRequestFilter {

  private final UserDetailsService userDetailsService;
  private final JwtUtil jwtUtil;
  private final UserService userService;
  private final JwtSecurityService jwtSecurityService;

  @Value("${jwt.rotation.minutes:30}")
  private int rotationMinutes;

  private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

  public JwtRequestFilter(
      UserDetailsService userDetailsService,
      JwtUtil jwtUtil,
      UserService userService,
      JwtSecurityService jwtSecurityService) {
    this.userDetailsService = userDetailsService;
    this.jwtUtil = jwtUtil;
    this.userService = userService;
    this.jwtSecurityService = jwtSecurityService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    log.debug("Request URI: {}", request.getRequestURI());
    final String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }

    String jwt = authorizationHeader.substring(7);

    // ============================================
    // DEFENSE LAYER 1: BLACKLIST CHECK
    // ============================================
    if (jwtSecurityService.isTokenBlacklisted(jwt)) {
      log.warn("Blocked request with revoked token");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Token revoked\",\"code\":\"TOKEN_REVOKED\"}");
      return;
    }

    try {
      String username = jwtUtil.extractUsername(jwt);
      log.debug("Extracted username: {}", username);

      // ============================================
      // DEFENSE LAYER 2: FINGERPRINT VALIDATION
      // ============================================
      if (!jwtSecurityService.validateTokenFingerprint(jwt, request)) {
        log.warn("Token fingerprint mismatch for user: {} - possible token theft!", username);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Invalid token context\",\"code\":\"FINGERPRINT_MISMATCH\"}");
        return;
      }

      // ============================================
      // DEFENSE LAYER 3: STANDARD JWT VALIDATION
      // ============================================
      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        // Atomic authentication via UserService (DB lock + status check)
        boolean authenticated = userService.verifyAndAuthenticate(
            username,
            jwt,
            userDetails,
            new WebAuthenticationDetailsSource().buildDetails(request),
            jwtUtil);

        if (authenticated) {
          // ============================================
          // DEFENSE LAYER 4: AUTO-ROTATION
          // ============================================
          if (jwtSecurityService.shouldRotateToken(jwt)) {
            String newToken = jwtSecurityService.rotateToken(jwt, request);
            response.setHeader("X-New-Token", newToken);
            log.info("Token rotated for user: {}", username);
          }
        }
      }

    } catch (Exception e) {
      log.error("JWT validation failed: {}", e.getMessage());
    }

    chain.doFilter(request, response);
  }
}
