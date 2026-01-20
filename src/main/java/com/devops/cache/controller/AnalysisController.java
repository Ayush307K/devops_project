package com.devops.cache.controller;

import com.devops.cache.dto.DriftReport;
import com.devops.cache.model.InvalidationEvent;
import com.devops.cache.service.ConsistencyAnalyzer;
import com.devops.cache.service.DBEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for consistency analysis operations.
 */
@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final ConsistencyAnalyzer consistencyAnalyzer;
    private final DBEngine dbEngine;

    /**
     * Generates a comprehensive drift report.
     */
    @GetMapping("/drift")
    public ResponseEntity<DriftReport> analyzeDrift(
            @RequestParam(defaultValue = "false") boolean autoFix) {

        log.info("Analyzing cache-database drift. AutoFix: {}", autoFix);
        DriftReport report = consistencyAnalyzer.analyzeDrift(autoFix);
        return ResponseEntity.ok(report);
    }

    /**
     * Gets a quick drift summary without full analysis.
     */
    @GetMapping("/drift/summary")
    public ResponseEntity<Map<String, Object>> getQuickDriftSummary() {
        log.info("Fetching quick drift summary");
        Map<String, Object> summary = consistencyAnalyzer.getQuickDriftSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Checks if a specific record has stale cache.
     */
    @GetMapping("/stale/{id}")
    public ResponseEntity<Map<String, Object>> checkIfStale(@PathVariable Long id) {
        log.info("Checking staleness for record id: {}", id);
        boolean isStale = consistencyAnalyzer.isRecordStale(id);

        return ResponseEntity.ok(Map.of(
                "recordId", id,
                "isStale", isStale,
                "message", isStale ? "Cache is stale" : "Cache is consistent"
        ));
    }

    /**
     * Forces a cache refresh for a specific record.
     */
    @PostMapping("/refresh/{id}")
    public ResponseEntity<Map<String, String>> forceRefresh(@PathVariable Long id) {
        log.info("Forcing cache refresh for record id: {}", id);

        try {
            consistencyAnalyzer.forceRefresh(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Cache refreshed successfully",
                    "recordId", id.toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves all invalidation events.
     */
    @GetMapping("/events")
    public ResponseEntity<List<InvalidationEvent>> getAllInvalidationEvents() {
        log.info("Fetching all invalidation events");
        List<InvalidationEvent> events = dbEngine.getAllInvalidationEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Retrieves recent invalidation events.
     */
    @GetMapping("/events/recent")
    public ResponseEntity<List<InvalidationEvent>> getRecentInvalidationEvents() {
        log.info("Fetching recent invalidation events");
        List<InvalidationEvent> events = dbEngine.getRecentInvalidationEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Gets invalidation statistics.
     */
    @GetMapping("/events/stats")
    public ResponseEntity<Map<String, Object>> getInvalidationStats() {
        log.info("Fetching invalidation statistics");

        long totalEvents = dbEngine.getAllInvalidationEvents().size();
        long failedEvents = dbEngine.countFailedInvalidations();
        double failureRate = totalEvents == 0 ? 0.0 : (failedEvents * 100.0) / totalEvents;

        Map<String, Object> stats = Map.of(
                "totalInvalidationAttempts", totalEvents,
                "failedInvalidations", failedEvents,
                "successfulInvalidations", totalEvents - failedEvents,
                "failureRate", String.format("%.2f%%", failureRate)
        );

        return ResponseEntity.ok(stats);
    }
}
