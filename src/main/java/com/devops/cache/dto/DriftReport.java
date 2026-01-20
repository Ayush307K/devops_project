package com.devops.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing cache-database consistency analysis report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftReport {

    private Long totalRecords;

    private Long cachedRecords;

    private Long staleRecords;

    private Double driftScore;

    private Long autoFixedCount;

    private SystemVerdict verdict;

    private LocalDateTime generatedAt;

    private List<StalenessDetail> staleDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StalenessDetail {
        private Long recordId;
        private Long dbVersion;
        private Long cacheVersion;
        private Long versionDrift;
        private boolean autoFixed;
    }

    public enum SystemVerdict {
        HEALTHY("0-10% drift - System is healthy"),
        MINOR_DRIFT("11-30% drift - Minor inconsistencies detected"),
        RISK("31-60% drift - Significant risk of stale data"),
        CRITICAL("61-100% drift - Critical consistency failure");

        private final String description;

        SystemVerdict(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static SystemVerdict fromDriftScore(double score) {
            if (score <= 10) return HEALTHY;
            if (score <= 30) return MINOR_DRIFT;
            if (score <= 60) return RISK;
            return CRITICAL;
        }
    }

    /**
     * Calculates drift score based on staleness.
     */
    public void calculateDriftScore() {
        if (totalRecords == 0) {
            this.driftScore = 0.0;
        } else {
            this.driftScore = (staleRecords * 100.0) / totalRecords;
        }
        this.verdict = SystemVerdict.fromDriftScore(this.driftScore);
    }
}
