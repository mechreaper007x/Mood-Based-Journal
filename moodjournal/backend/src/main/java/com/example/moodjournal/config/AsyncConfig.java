package com.example.moodjournal.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Async configuration that propagates Spring Security context to async threads.
 * This allows @Async methods to access the authenticated user's security
 * context.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Use INHERITABLE_THREAD_LOCAL mode so security context is inherited by child
     * threads.
     * This is set at application startup.
     */
    static {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    /**
     * Custom task executor wrapped with DelegatingSecurityContextExecutor
     * to propagate security context to async threads.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();

        // Wrap with security context delegation
        return new DelegatingSecurityContextExecutor(executor);
    }
}
