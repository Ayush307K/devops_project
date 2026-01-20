package com.devops.cache.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a cache invalidation attempt.
 * Tracks success/failure of invalidation operations.
 */
@Entity
@Table(name = "invalidation_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "db_version", nullable = false)
    private Long dbVersion;

    @Column(name = "cache_version")
    private Long cacheVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvalidationStatus status;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum InvalidationStatus {
        SUCCESS,
        FAILED,
        PARTIAL,
        SKIPPED
    }
}
