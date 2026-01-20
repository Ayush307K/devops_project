package com.devops.cache.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a database record with versioning support.
 * Each update increments the version number to track consistency.
 */
@Entity
@Table(name = "data_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_value", nullable = false, length = 1000)
    private String value;

    @Column(nullable = false)
    @Builder.Default
    private Long version = 1L;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Increments the version number on each update.
     */
    public void incrementVersion() {
        this.version++;
    }
}
