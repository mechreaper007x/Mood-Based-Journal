package com.example.moodjournal.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;






public interface TokenStore {

    


    void setWithExpiry(String key, String value, long ttlMillis);

    


    String get(String key);

    


    boolean exists(String key);

    


    void delete(String key);
}











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
