package com.devops.cache.controller;

import com.devops.cache.model.CacheRecord;
import com.devops.cache.service.CacheEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for cache operations.
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheEngine cacheEngine;

    /**
     * Retrieves a cached record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CacheRecord> getCachedRecord(@PathVariable Long id) {
        log.info("Fetching cached record id: {}", id);
        Optional<CacheRecord> record = cacheEngine.get(id);
        return record.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all cached records.
     */
    @GetMapping("/all")
    public ResponseEntity<Map<Long, CacheRecord>> getAllCached() {
        log.info("Fetching all cached records");
        Map<Long, CacheRecord> cached = cacheEngine.getAllCached();
        return ResponseEntity.ok(cached);
    }

    /**
     * Invalidates a cached entry.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> invalidateCache(@PathVariable Long id) {
        log.info("Invalidating cache for id: {}", id);
        boolean success = cacheEngine.invalidate(id);

        Map<String, Object> response = Map.of(
                "id", id,
                "invalidated", success,
                "message", success ? "Cache invalidated successfully" : "Invalidation failed"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Clears all cache entries.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        log.info("Clearing all cache entries");
        cacheEngine.clearAll();
        return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
    }

    /**
     * Gets cache statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        log.info("Fetching cache statistics");
        Map<String, Object> stats = cacheEngine.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Updates cache failure rate for testing.
     */
    @PostMapping("/config/failure-rate")
    public ResponseEntity<Map<String, Object>> updateFailureRate(@RequestParam double rate) {
        log.info("Updating cache failure rate to: {}", rate);

        if (rate < 0.0 || rate > 1.0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failure rate must be between 0.0 and 1.0"));
        }

        cacheEngine.setFailureRate(rate);

        return ResponseEntity.ok(Map.of(
                "message", "Failure rate updated",
                "newRate", rate
        ));
    }
}
