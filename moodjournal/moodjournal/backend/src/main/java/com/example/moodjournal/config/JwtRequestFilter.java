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

    
    
    
    if (jwtSecurityService.isTokenBlacklisted(jwt)) {
      log.warn("Blocked request with revoked token");
      writeUnauthorized(response, "Token revoked", "TOKEN_REVOKED");
      return;
    }

    if (!jwtSecurityService.validateTokenBinding(jwt)) {
      log.warn("Blocked request with invalid token binding");
      writeUnauthorized(response, "Invalid token", "TOKEN_BINDING_INVALID");
      return;
    }

    try {
      String username = jwtUtil.extractUsername(jwt);
      if (username == null || username.isBlank()) {
        writeUnauthorized(response, "Invalid token", "TOKEN_INVALID");
        return;
      }
      log.debug("Extracted username: {}", username);

      
      
      
      if (!jwtSecurityService.validateTokenFingerprint(jwt, request)) {
        log.warn("Token fingerprint mismatch for user: {} - possible token theft!", username);
        writeUnauthorized(response, "Invalid token context", "FINGERPRINT_MISMATCH");
        return;
      }

      
      
      
      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        
        boolean authenticated = userService.verifyAndAuthenticate(
            username,
            jwt,
            userDetails,
            new WebAuthenticationDetailsSource().buildDetails(request),
            jwtUtil);

        if (!authenticated) {
          writeUnauthorized(response, "Invalid token", "TOKEN_VALIDATION_FAILED");
          return;
        }

        if (authenticated) {
          
          
          
          if (jwtSecurityService.shouldRotateToken(jwt)) {
            String newToken = jwtSecurityService.rotateToken(jwt, request);
            response.setHeader("X-New-Token", newToken);
            log.info("Token rotated for user: {}", username);
          }
        }
      }

    } catch (Exception e) {
      log.error("JWT validation failed: {}", e.getMessage());
      writeUnauthorized(response, "Invalid token", "TOKEN_INVALID");
      return;
    }

    chain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse response, String error, String code) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write(String.format("{\"error\":\"%s\",\"code\":\"%s\"}", error, code));
  }
}
