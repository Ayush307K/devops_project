package com.devops.cache.repository;

import com.devops.cache.model.InvalidationEvent;
import com.devops.cache.model.InvalidationEvent.InvalidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for InvalidationEvent entity.
 */
@Repository
public interface InvalidationEventRepository extends JpaRepository<InvalidationEvent, Long> {

    /**
     * Finds all invalidation events for a specific record.
     */
    List<InvalidationEvent> findByRecordId(Long recordId);

    /**
     * Finds events by status.
     */
    List<InvalidationEvent> findByStatus(InvalidationStatus status);

    /**
     * Finds events within a time range.
     */
    List<InvalidationEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Counts failed invalidation attempts.
     */
    long countByStatus(InvalidationStatus status);

    /**
     * Finds recent events ordered by timestamp descending.
     */
    List<InvalidationEvent> findTop10ByOrderByTimestampDesc();
}
