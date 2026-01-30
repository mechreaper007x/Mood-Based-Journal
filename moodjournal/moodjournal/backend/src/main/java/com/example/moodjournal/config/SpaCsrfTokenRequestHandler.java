package com.example.moodjournal.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

/**
 * Custom CSRF token handler for Single-Page Applications.
 * 
 * This handler is required for SPAs because:
 * 1. Spring Security 6 provides BREACH protection by XOR-encoding tokens
 * 2. The cookie contains the RAW token (for JS to read)
 * 3. The header can contain either RAW or XOR-encoded token
 * 4. This handler correctly resolves both cases
 * 
 * From Spring Security 6.5 documentation:
 * https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            Supplier<CsrfToken> csrfToken) {
        /*
         * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection
         * of
         * the CsrfToken when it is rendered in the response body.
         */
        this.xor.handle(request, response, csrfToken);

        /*
         * Render the token value to a cookie by causing the deferred token to be
         * loaded.
         * This ensures the XSRF-TOKEN cookie is always set.
         */
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());

        /*
         * If the request contains a request header, use
         * CsrfTokenRequestAttributeHandler
         * to resolve the CsrfToken. This applies when a single-page application
         * includes
         * the header value automatically, which was obtained via a cookie containing
         * the
         * raw CsrfToken.
         */
        if (headerValue != null) {
            // SPA sent the raw cookie value in header - use plain handler
            return this.plain.resolveCsrfTokenValue(request, csrfToken);
        }

        /*
         * In all other cases (e.g., form submission), use XOR handler to decode the
         * BREACH-protected token.
         */
        return this.xor.resolveCsrfTokenValue(request, csrfToken);
    }
}
