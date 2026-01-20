package com.devops.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a cached entry in the in-memory cache.
 * Stores version information to detect staleness.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CacheRecord {

    private Long id;

    private String value;

    private Long version;

    private LocalDateTime cachedAt;

    private LocalDateTime expiresAt;

    /**
     * Checks if the cache entry has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if this cache record is stale compared to database version.
     */
    public boolean isStale(Long dbVersion) {
        return this.version < dbVersion;
    }
}
