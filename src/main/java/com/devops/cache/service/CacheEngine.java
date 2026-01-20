package com.devops.cache.service;

import com.devops.cache.model.CacheRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache engine simulating Redis behavior.
 * Supports GET, PUT, INVALIDATE operations with failure simulation.
 */
@Service
@Slf4j
public class CacheEngine {

    private final Map<Long, CacheRecord> cache = new ConcurrentHashMap<>();

    @Value("${cache.simulation.failure-rate:0.2}")
    private double failureRate;

    @Value("${cache.simulation.network-delay-ms:100}")
    private int networkDelayMs;

    @Value("${cache.ttl.default-seconds:300}")
    private int defaultTtlSeconds;

    private final Random random = new Random();

    /**
     * Retrieves a cached record by ID.
     */
    public Optional<CacheRecord> get(Long id) {
        simulateNetworkDelay();

        CacheRecord record = cache.get(id);

        if (record == null) {
            log.debug("Cache MISS for id: {}", id);
            return Optional.empty();
        }

        if (record.isExpired()) {
            log.info("Cache entry EXPIRED for id: {}", id);
            cache.remove(id);
            return Optional.empty();
        }

        log.debug("Cache HIT for id: {}, version: {}", id, record.getVersion());
        return Optional.of(record);
    }

    /**
     * Puts a record into the cache.
     */
    public void put(Long id, String value, Long version) {
        simulateNetworkDelay();

        LocalDateTime now = LocalDateTime.now();
        CacheRecord record = CacheRecord.builder()
                .id(id)
                .value(value)
                .version(version)
                .cachedAt(now)
                .expiresAt(now.plusSeconds(defaultTtlSeconds))
                .build();

        cache.put(id, record);
        log.info("Cached record id: {}, version: {}", id, version);
    }

    /**
     * Invalidates (removes) a cached entry.
     * May randomly fail to simulate real-world scenarios.
     */
    public boolean invalidate(Long id) {
        simulateNetworkDelay();

        if (shouldSimulateFailure()) {
            log.warn("SIMULATED FAILURE: Cache invalidation failed for id: {}", id);
            return false;
        }

        CacheRecord removed = cache.remove(id);
        if (removed != null) {
            log.info("Cache invalidated for id: {}, previous version: {}", id, removed.getVersion());
            return true;
        }

        log.debug("Cache invalidation skipped - id: {} not in cache", id);
        return true;
    }

    /**
     * Gets all cached records.
     */
    public Map<Long, CacheRecord> getAllCached() {
        return new HashMap<>(cache);
    }

    /**
     * Clears all cache entries.
     */
    public void clearAll() {
        int size = cache.size();
        cache.clear();
        log.info("Cache cleared. Removed {} entries", size);
    }

    /**
     * Gets cache statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", cache.size());
        stats.put("failureRate", failureRate);
        stats.put("networkDelayMs", networkDelayMs);
        stats.put("defaultTtlSeconds", defaultTtlSeconds);

        long expiredCount = cache.values().stream()
                .filter(CacheRecord::isExpired)
                .count();
        stats.put("expiredEntries", expiredCount);

        return stats;
    }

    /**
     * Checks if cache contains a specific ID.
     */
    public boolean contains(Long id) {
        return cache.containsKey(id) && !cache.get(id).isExpired();
    }

    /**
     * Simulates network delay.
     */
    private void simulateNetworkDelay() {
        if (networkDelayMs > 0) {
            try {
                Thread.sleep(networkDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Randomly determines if operation should fail.
     */
    private boolean shouldSimulateFailure() {
        return random.nextDouble() < failureRate;
    }

    /**
     * Updates the failure rate (for testing purposes).
     */
    public void setFailureRate(double rate) {
        this.failureRate = Math.max(0.0, Math.min(1.0, rate));
        log.info("Cache failure rate updated to: {}", this.failureRate);
    }
}
