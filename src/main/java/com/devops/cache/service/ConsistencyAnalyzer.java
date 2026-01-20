package com.devops.cache.service;

import com.devops.cache.dto.DriftReport;
import com.devops.cache.model.CacheRecord;
import com.devops.cache.model.DataRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for analyzing cache-database consistency and detecting drift.
 * Core component of the system.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConsistencyAnalyzer {

    private final DBEngine dbEngine;
    private final CacheEngine cacheEngine;

    /**
     * Generates a comprehensive drift report.
     */
    public DriftReport analyzeDrift(boolean autoFix) {
        log.info("Starting consistency analysis. AutoFix enabled: {}", autoFix);

        List<DataRecord> allDbRecords = dbEngine.getAllRecords();
        Map<Long, CacheRecord> allCachedRecords = cacheEngine.getAllCached();

        long totalRecords = allDbRecords.size();
        long cachedRecords = allCachedRecords.size();
        long staleRecords = 0;
        long autoFixedCount = 0;

        List<DriftReport.StalenessDetail> staleDetails = new ArrayList<>();

        for (DataRecord dbRecord : allDbRecords) {
            Optional<CacheRecord> cachedRecordOpt = cacheEngine.get(dbRecord.getId());

            if (cachedRecordOpt.isEmpty()) {
                log.debug("Record {} not in cache", dbRecord.getId());
                continue;
            }

            CacheRecord cachedRecord = cachedRecordOpt.get();

            if (cachedRecord.isStale(dbRecord.getVersion())) {
                staleRecords++;

                long versionDrift = dbRecord.getVersion() - cachedRecord.getVersion();

                log.warn("STALE DATA DETECTED: id={}, dbVersion={}, cacheVersion={}, drift={}",
                        dbRecord.getId(), dbRecord.getVersion(), cachedRecord.getVersion(), versionDrift);

                boolean fixed = false;
                if (autoFix) {
                    fixed = performAutoFix(dbRecord);
                    if (fixed) {
                        autoFixedCount++;
                    }
                }

                DriftReport.StalenessDetail detail = DriftReport.StalenessDetail.builder()
                        .recordId(dbRecord.getId())
                        .dbVersion(dbRecord.getVersion())
                        .cacheVersion(cachedRecord.getVersion())
                        .versionDrift(versionDrift)
                        .autoFixed(fixed)
                        .build();

                staleDetails.add(detail);
            }
        }

        DriftReport report = DriftReport.builder()
                .totalRecords(totalRecords)
                .cachedRecords(cachedRecords)
                .staleRecords(staleRecords)
                .autoFixedCount(autoFixedCount)
                .generatedAt(LocalDateTime.now())
                .staleDetails(staleDetails)
                .build();

        report.calculateDriftScore();

        log.info("Consistency analysis complete. Drift Score: {}%, Verdict: {}",
                String.format("%.2f", report.getDriftScore()), report.getVerdict());

        return report;
    }

    /**
     * Performs auto-fix by refreshing stale cache entry from database.
     */
    private boolean performAutoFix(DataRecord dbRecord) {
        try {
            cacheEngine.put(dbRecord.getId(), dbRecord.getValue(), dbRecord.getVersion());
            log.info("AUTO-FIX: Refreshed cache for record id={}, version={}",
                    dbRecord.getId(), dbRecord.getVersion());
            return true;
        } catch (Exception e) {
            log.error("AUTO-FIX FAILED for record id={}: {}", dbRecord.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Detects if a specific record has stale cache.
     */
    public boolean isRecordStale(Long id) {
        Optional<DataRecord> dbRecordOpt = dbEngine.getRecord(id);
        if (dbRecordOpt.isEmpty()) {
            return false;
        }

        Optional<CacheRecord> cacheRecordOpt = cacheEngine.get(id);
        if (cacheRecordOpt.isEmpty()) {
            return false;
        }

        DataRecord dbRecord = dbRecordOpt.get();
        CacheRecord cacheRecord = cacheRecordOpt.get();

        return cacheRecord.isStale(dbRecord.getVersion());
    }

    /**
     * Forces a consistency check and refresh for a specific record.
     */
    public void forceRefresh(Long id) {
        Optional<DataRecord> dbRecordOpt = dbEngine.getRecord(id);

        if (dbRecordOpt.isEmpty()) {
            throw new IllegalArgumentException("Record not found in database: " + id);
        }

        DataRecord dbRecord = dbRecordOpt.get();
        cacheEngine.put(dbRecord.getId(), dbRecord.getValue(), dbRecord.getVersion());

        log.info("Forced cache refresh for record id={}", id);
    }

    /**
     * Gets a quick drift summary without full analysis.
     */
    public Map<String, Object> getQuickDriftSummary() {
        List<DataRecord> allDbRecords = dbEngine.getAllRecords();
        long totalRecords = allDbRecords.size();
        long staleCount = allDbRecords.stream()
                .filter(dbRecord -> {
                    Optional<CacheRecord> cached = cacheEngine.get(dbRecord.getId());
                    return cached.isPresent() && cached.get().isStale(dbRecord.getVersion());
                })
                .count();

        double driftScore = totalRecords == 0 ? 0.0 : (staleCount * 100.0) / totalRecords;

        return Map.of(
                "totalRecords", totalRecords,
                "staleRecords", staleCount,
                "driftScore", driftScore,
                "verdict", DriftReport.SystemVerdict.fromDriftScore(driftScore).name()
        );
    }
}
