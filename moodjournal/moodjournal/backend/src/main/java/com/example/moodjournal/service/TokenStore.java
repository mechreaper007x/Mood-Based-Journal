package com.example.moodjournal.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Token storage abstraction for JWT security operations.
 * This interface is Redis-compatible - swap InMemoryTokenStore with
 * RedisTokenStore for production scaling.
 */
public interface TokenStore {

    /**
     * Store a value with automatic expiry.
     */
    void setWithExpiry(String key, String value, long ttlMillis);

    /**
     * Retrieve a stored value.
     */
    String get(String key);

    /**
     * Check if a key exists.
     */
    boolean exists(String key);

    /**
     * Delete a key.
     */
    void delete(String key);
}

/**
 * In-memory implementation of TokenStore using ConcurrentHashMap.
 * Thread-safe with automatic expiry cleanup.
 * 
 * For production at scale, replace with RedisTokenStore:
 * 
 * @Bean TokenStore tokenStore(RedisTemplate<String, String> redis) {
 *       return new RedisTokenStore(redis);
 *       }
 */
@Component
class InMemoryTokenStore implements TokenStore {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expiryTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "token-store-cleanup");
        t.setDaemon(true);
        return t;
    });

    public InMemoryTokenStore() {
        // Cleanup expired entries every 60 seconds
        scheduler.scheduleAtFixedRate(this::cleanup, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public void setWithExpiry(String key, String value, long ttlMillis) {
        store.put(key, value);
        expiryTimes.put(key, System.currentTimeMillis() + ttlMillis);
    }

    @Override
    public String get(String key) {
        Long expiry = expiryTimes.get(key);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            delete(key);
            return null;
        }
        return store.get(key);
    }

    @Override
    public boolean exists(String key) {
        Long expiry = expiryTimes.get(key);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            delete(key);
            return false;
        }
        return store.containsKey(key);
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        expiryTimes.remove(key);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        expiryTimes.forEach((key, expiry) -> {
            if (now > expiry) {
                delete(key);
            }
        });
    }
}
