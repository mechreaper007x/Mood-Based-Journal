package com.example.moodjournal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that ensures CSRF token is loaded and available for stateless
 * applications.
 * 
 * In Spring Security 6, the CSRF token is lazily loaded by default. This filter
 * forces the token to be loaded on every request, ensuring the XSRF-TOKEN
 * cookie
 * is always set in the response.
 * 
 * This is required for stateless SPAs that use the Double Submit Cookie
 * pattern.
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Get the CSRF token from request attributes (set by Spring Security)
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        // Force the token to be loaded so the cookie gets set in the response
        // In Spring Security 6, accessing the token triggers cookie generation
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}
