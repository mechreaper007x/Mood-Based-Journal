package com.example.moodjournal.config;

import com.example.moodjournal.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.example.moodjournal.service.UserService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtRequestFilter extends OncePerRequestFilter {

  private final UserDetailsService userDetailsService;
  private final JwtUtil jwtUtil;
  private final UserService userService;
  private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

  public JwtRequestFilter(
      UserDetailsService userDetailsService,
      JwtUtil jwtUtil,
      UserService userService) {
    this.userDetailsService = userDetailsService;
    this.jwtUtil = jwtUtil;
    this.userService = userService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    log.info("Request URI: {}", request.getRequestURI());
    final String authorizationHeader = request.getHeader("Authorization");

    String username = null;
    String jwt = null;

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      jwt = authorizationHeader.substring(7);
      try {
        username = jwtUtil.extractUsername(jwt);
        log.info("Extracted username: {}", username);
      } catch (Exception e) {
        log.error("Error extracting username from token", e);
      }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

      // V7 Fix: Strict Atomic Authentication via Service
      // The Service holds the DB lock AND sets the SecurityContext within the same
      // transaction.
      userService.verifyAndAuthenticate(
          username,
          jwt,
          userDetails,
          new WebAuthenticationDetailsSource().buildDetails(request),
          jwtUtil);
    }

    chain.doFilter(request, response);
  }
}
