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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

  private final UserDetailsService userDetailsService;
  private final JwtUtil jwtUtil;
  private static final Logger log = LoggerFactory.getLogger(
    JwtRequestFilter.class
  );

  public JwtRequestFilter(
    UserDetailsService userDetailsService,
    JwtUtil jwtUtil
  ) {
    this.userDetailsService = userDetailsService;
    this.jwtUtil = jwtUtil;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  ) throws ServletException, IOException {
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

    if (
      username != null && SecurityContextHolder.getContext().getAuthentication() == null
    ) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(
          username
        );

      boolean isTokenValid = jwtUtil.validateToken(jwt, userDetails.getUsername());
      log.info("Is token valid? {}", isTokenValid);

      if (isTokenValid) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          userDetails.getAuthorities()
        );
        usernamePasswordAuthenticationToken.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder
          .getContext()
          .setAuthentication(usernamePasswordAuthenticationToken);
        log.info(
          "User authenticated: {}, authorities: {}",
          username,
          userDetails.getAuthorities()
        );
      }
    }
    chain.doFilter(request, response);
  }
}
