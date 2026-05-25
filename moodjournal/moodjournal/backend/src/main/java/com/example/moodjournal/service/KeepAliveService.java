package com.example.moodjournal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service to prevent cold starts on Render free tier by pinging the backend and frontend
 * every 14 minutes. Render spins down free web services after 15 minutes of inactivity.
 * By making an outgoing request to the public URL, the request is routed through the load
 * balancer and counts as incoming HTTP traffic, resetting the idle timer.
 */
@Service
public class KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveService.class);
    private final RestTemplate restTemplate;

    @Value("${RENDER_EXTERNAL_URL:}")
    private String backendUrl;

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    public KeepAliveService() {
        this.restTemplate = new RestTemplate();
    }

    // Ping every 14 minutes (840000 ms)
    @Scheduled(fixedRate = 840000)
    public void pingServices() {
        if (backendUrl != null && !backendUrl.trim().isEmpty()) {
            try {
                logger.info("Sending keep-alive ping to backend: {}", backendUrl);
                restTemplate.getForObject(backendUrl, String.class);
            } catch (Exception e) {
                // Ignore errors like 404, the request itself is enough to keep the service awake
                logger.debug("Backend ping returned error (expected if no root endpoint): {}", e.getMessage());
            }
        } else {
            logger.debug("No RENDER_EXTERNAL_URL defined, skipping backend ping.");
        }

        if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
            try {
                logger.info("Sending keep-alive ping to frontend: {}", frontendUrl);
                restTemplate.getForObject(frontendUrl, String.class);
            } catch (Exception e) {
                logger.debug("Frontend ping returned error: {}", e.getMessage());
            }
        } else {
            logger.debug("No FRONTEND_URL defined, skipping frontend ping.");
        }
    }
}
