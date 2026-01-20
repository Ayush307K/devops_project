package com.devops.cache.service;

import com.devops.cache.model.DataRecord;
import com.devops.cache.model.InvalidationEvent;
import com.devops.cache.repository.DataRecordRepository;
import com.devops.cache.repository.InvalidationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Database engine service handling all database operations.
 * Coordinates with cache invalidation logic.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DBEngine {

    private final DataRecordRepository dataRecordRepository;
    private final InvalidationEventRepository invalidationEventRepository;
    private final CacheEngine cacheEngine;

    /**
     * Creates a new record in the database.
     */
    @Transactional
    public DataRecord createRecord(String value, boolean cacheImmediately) {
        DataRecord record = DataRecord.builder()
                .value(value)
                .version(1L)
                .lastUpdated(LocalDateTime.now())
                .build();

        DataRecord saved = dataRecordRepository.save(record);
        log.info("Created new record: id={}, version={}", saved.getId(), saved.getVersion());

        if (cacheImmediately) {
            cacheEngine.put(saved.getId(), saved.getValue(), saved.getVersion());
        }

        return saved;
    }

    /**
     * Updates an existing record and handles cache invalidation.
     */
    @Transactional
    public DataRecord updateRecord(Long id, String newValue, boolean invalidateCache, boolean simulateFailure) {
        Optional<DataRecord> optionalRecord = dataRecordRepository.findById(id);

        if (optionalRecord.isEmpty()) {
            throw new IllegalArgumentException("Record not found with id: " + id);
        }

        DataRecord record = optionalRecord.get();
        Long oldVersion = record.getVersion();

        record.setValue(newValue);
        record.incrementVersion();
        record.setLastUpdated(LocalDateTime.now());

        DataRecord updated = dataRecordRepository.save(record);
        log.info("Updated record: id={}, version={}->{}", id, oldVersion, updated.getVersion());

        if (invalidateCache) {
            handleCacheInvalidation(updated, simulateFailure);
        }

        return updated;
    }

    /**
     * Retrieves a record by ID.
     */
    public Optional<DataRecord> getRecord(Long id) {
        return dataRecordRepository.findById(id);
    }

    /**
     * Retrieves all records.
     */
    public List<DataRecord> getAllRecords() {
        return dataRecordRepository.findAll();
    }

    /**
     * Deletes a record by ID.
     */
    @Transactional
    public void deleteRecord(Long id) {
        dataRecordRepository.deleteById(id);
        cacheEngine.invalidate(id);
        log.info("Deleted record: id={}", id);
    }

    /**
     * Handles cache invalidation and logs the event.
     */
    private void handleCacheInvalidation(DataRecord record, boolean forceFailure) {
        Long cacheVersion = null;
        Optional<com.devops.cache.model.CacheRecord> cachedRecord = cacheEngine.get(record.getId());
        if (cachedRecord.isPresent()) {
            cacheVersion = cachedRecord.get().getVersion();
        }

        boolean invalidationSuccess;
        String reason;

        if (forceFailure) {
            invalidationSuccess = false;
            reason = "Simulated failure";
            log.warn("Cache invalidation FORCED to fail for record: {}", record.getId());
        } else {
            invalidationSuccess = cacheEngine.invalidate(record.getId());
            reason = invalidationSuccess ? "Normal invalidation" : "Invalidation failed";
        }

        InvalidationEvent.InvalidationStatus status = invalidationSuccess
                ? InvalidationEvent.InvalidationStatus.SUCCESS
                : InvalidationEvent.InvalidationStatus.FAILED;

        InvalidationEvent event = InvalidationEvent.builder()
                .recordId(record.getId())
                .dbVersion(record.getVersion())
                .cacheVersion(cacheVersion)
                .status(status)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

        invalidationEventRepository.save(event);
        log.info("Logged invalidation event: recordId={}, status={}", record.getId(), status);
    }

    /**
     * Retrieves all invalidation events.
     */
    public List<InvalidationEvent> getAllInvalidationEvents() {
        return invalidationEventRepository.findAll();
    }

    /**
     * Retrieves recent invalidation events.
     */
    public List<InvalidationEvent> getRecentInvalidationEvents() {
        return invalidationEventRepository.findTop10ByOrderByTimestampDesc();
    }

    /**
     * Counts failed invalidations.
     */
    public long countFailedInvalidations() {
        return invalidationEventRepository.countByStatus(InvalidationEvent.InvalidationStatus.FAILED);
    }
}
