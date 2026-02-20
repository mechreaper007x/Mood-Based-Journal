package com.example.moodjournal.config;

import com.example.moodjournal.model.User;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> AUTH_RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password");
    private static final String AI_ROUTE_PREFIX = "/api/ai";

    private static final int REQUESTS_PER_MINUTE = 10;
    private static final int MAX_BUCKETS = 2_000;
    private static final long STALE_BUCKET_TTL_MILLIS = Duration.ofMinutes(30).toMillis();

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final ReentrantLock evictionLock = new ReentrantLock();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {

        String path = normalizePath(request.getRequestURI(), request.getContextPath());
        String bucketKey = null;

        if (isAuthRateLimitedPath(path)) {
            bucketKey = "auth:ip:" + getClientIP(request);
        } else if (isAiRateLimitedPath(path)) {
            bucketKey = "ai:user:" + resolveAiIdentity(request);
        }

        if (bucketKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = resolveBucket(bucketKey);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.addHeader("Retry-After", "60");
        response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
    }

    private boolean isAuthRateLimitedPath(String path) {
        return AUTH_RATE_LIMITED_PATHS.contains(path);
    }

    private boolean isAiRateLimitedPath(String path) {
        return path.equals(AI_ROUTE_PREFIX) || path.startsWith(AI_ROUTE_PREFIX + "/");
    }

    private Bucket resolveBucket(String key) {
        BucketEntry entry = buckets.compute(key, (unused, existing) -> {
            if (existing == null) {
                return new BucketEntry(createNewBucket());
            }
            existing.touch();
            return existing;
        });
        evictIfNecessary();
        return entry.bucket();
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE,
                Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveAiIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User user) {
                if (user.getId() != null) {
                    return "id:" + user.getId();
                }
                if (StringUtils.hasText(user.getUsername())) {
                    return "username:" + user.getUsername();
                }
            }

            if (principal instanceof UserDetails userDetails && StringUtils.hasText(userDetails.getUsername())) {
                return "username:" + userDetails.getUsername();
            }

            if (principal instanceof String principalText
                    && StringUtils.hasText(principalText)
                    && !"anonymousUser".equalsIgnoreCase(principalText)) {
                return "username:" + principalText;
            }

            String authName = authentication.getName();
            if (StringUtils.hasText(authName) && !"anonymousUser".equalsIgnoreCase(authName)) {
                return "username:" + authName;
            }
        }

        return "anonymous-ip:" + getClientIP(request);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizePath(String requestUri, String contextPath) {
        String normalized = requestUri;
        if (StringUtils.hasText(contextPath) && normalized.startsWith(contextPath)) {
            normalized = normalized.substring(contextPath.length());
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void evictIfNecessary() {
        if (buckets.size() <= MAX_BUCKETS) {
            return;
        }

        evictionLock.lock();
        try {
            if (buckets.size() <= MAX_BUCKETS) {
                return;
            }

            long now = System.currentTimeMillis();
            for (Map.Entry<String, BucketEntry> entry : buckets.entrySet()) {
                if (buckets.size() <= MAX_BUCKETS) {
                    break;
                }
                if (now - entry.getValue().lastAccessEpochMillis() > STALE_BUCKET_TTL_MILLIS) {
                    buckets.remove(entry.getKey(), entry.getValue());
                }
            }

            while (buckets.size() > MAX_BUCKETS) {
                String oldestKey = null;
                long oldestAccess = Long.MAX_VALUE;
                for (Map.Entry<String, BucketEntry> entry : buckets.entrySet()) {
                    long access = entry.getValue().lastAccessEpochMillis();
                    if (access < oldestAccess) {
                        oldestAccess = access;
                        oldestKey = entry.getKey();
                    }
                }
                if (oldestKey == null) {
                    break;
                }
                buckets.remove(oldestKey);
            }
        } finally {
            evictionLock.unlock();
        }
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private final AtomicLong lastAccessEpochMillis = new AtomicLong(System.currentTimeMillis());

        private BucketEntry(Bucket bucket) {
            this.bucket = bucket;
        }

        private Bucket bucket() {
            touch();
            return bucket;
        }

        private void touch() {
            lastAccessEpochMillis.set(System.currentTimeMillis());
        }

        private long lastAccessEpochMillis() {
            return lastAccessEpochMillis.get();
        }
    }
}
