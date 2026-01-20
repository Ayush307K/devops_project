package com.devops.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Cache Invalidation Consistency Checker.
 * A system to detect and analyze cache-database consistency drift.
 */
@SpringBootApplication
public class CacheConsistencyCheckerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheConsistencyCheckerApplication.class, args);
    }
}
