package com.example.moodjournal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://mood-based-journal-1.onrender.com}")
        private String allowedOrigins;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new PasswordEncoder() {
                        private final PasswordEncoder bcrypt = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
                        private final PasswordEncoder argon2 = org.springframework.security.crypto.argon2.Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

                        @Override
                        public String encode(CharSequence rawPassword) {
                                return bcrypt.encode(rawPassword);
                        }

                        @Override
                        public boolean matches(CharSequence rawPassword, String encodedPassword) {
                                if (encodedPassword != null && encodedPassword.startsWith("$argon2")) {
                                        return argon2.matches(rawPassword, encodedPassword);
                                }
                                return bcrypt.matches(rawPassword, encodedPassword);
                        }
                        
                        @Override
                        public boolean upgradeEncoding(String encodedPassword) {
                                return encodedPassword != null && encodedPassword.startsWith("$argon2");
                        }
                };
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public JwtRequestFilter jwtRequestFilter(
                        org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
                        com.example.moodjournal.util.JwtUtil jwtUtil,
                        com.example.moodjournal.service.UserService userService,
                        com.example.moodjournal.service.JwtSecurityService jwtSecurityService) {
                return new JwtRequestFilter(userDetailsService, jwtUtil, userService, jwtSecurityService);
        }

        @Bean
        public RestTemplate restTemplate() {
                return new RestTemplate();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter)
                        throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(request -> {
                                        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                                        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                                        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                                        config.setAllowedHeaders(List.of("*"));
                                        config.setExposedHeaders(List.of("X-New-Token"));
                                        config.setAllowCredentials(false);
                                        return config;
                                }))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(
                                                                "/",
                                                                "/*.html",
                                                                "/login.html",
                                                                "/register.html",
                                                                "/community.html",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/static/**",
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/api/auth/forgot-password",
                                                                "/api/auth/reset-password",
                                                                "/api/auth/validate-reset-token",
                                                                "/api/ai/daily-quote")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .requestCache(requestCache -> requestCache.disable())
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .logout(logout -> logout.disable())
                                .headers(headers -> headers
                                                .xssProtection(xss -> xss.headerValue(
                                                                org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'; " +
                                                                                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://apis.google.com; "
                                                                                +
                                                                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                                                                +
                                                                                "img-src 'self' data: https:; " +
                                                                                "font-src 'self' https://fonts.gstatic.com; "
                                                                                +
                                                                                "connect-src 'self' http://localhost:5173 https://mood-based-journal-1.onrender.com; "
                                                                                +
                                                                                "frame-ancestors 'none';"))
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .preload(true)
                                                                .maxAgeInSeconds(31536000))
                                                .frameOptions(frame -> frame.deny())
                                                .contentTypeOptions(contentType -> {
                                                })
                                                .referrerPolicy(referrer -> referrer
                                                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

                http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
