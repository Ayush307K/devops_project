package com.devops.cache.service;

import com.devops.cache.model.CacheRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheEngine service.
 */
class CacheEngineTest {

    private CacheEngine cacheEngine;

    @BeforeEach
    void setUp() {
        cacheEngine = new CacheEngine();
        // Set network delay to 0 for faster tests
        ReflectionTestUtils.setField(cacheEngine, "networkDelayMs", 0);
        ReflectionTestUtils.setField(cacheEngine, "failureRate", 0.0);
        ReflectionTestUtils.setField(cacheEngine, "defaultTtlSeconds", 300);
    }

    @Test
    void testPutAndGet() {
        cacheEngine.put(1L, "test-value", 1L);

        Optional<CacheRecord> record = cacheEngine.get(1L);

        assertTrue(record.isPresent());
        assertEquals("test-value", record.get().getValue());
        assertEquals(1L, record.get().getVersion());
    }

    @Test
    void testGetNonExistent() {
        Optional<CacheRecord> record = cacheEngine.get(999L);
        assertFalse(record.isPresent());
    }

    @Test
    void testInvalidate() {
        cacheEngine.put(1L, "test-value", 1L);
        assertTrue(cacheEngine.contains(1L));

        boolean invalidated = cacheEngine.invalidate(1L);

        assertTrue(invalidated);
        assertFalse(cacheEngine.contains(1L));
    }

    @Test
    void testClearAll() {
        cacheEngine.put(1L, "value1", 1L);
        cacheEngine.put(2L, "value2", 1L);
        cacheEngine.put(3L, "value3", 1L);

        assertEquals(3, cacheEngine.getAllCached().size());

        cacheEngine.clearAll();

        assertEquals(0, cacheEngine.getAllCached().size());
    }

    @Test
    void testStalenessDetection() {
        cacheEngine.put(1L, "value", 1L);

        Optional<CacheRecord> record = cacheEngine.get(1L);
        assertTrue(record.isPresent());

        assertFalse(record.get().isStale(1L));
        assertTrue(record.get().isStale(2L));
        assertTrue(record.get().isStale(10L));
    }

    @Test
    void testFailureSimulation() {
        // Set high failure rate
        ReflectionTestUtils.setField(cacheEngine, "failureRate", 1.0);

        cacheEngine.put(1L, "value", 1L);

        boolean invalidated = cacheEngine.invalidate(1L);

        assertFalse(invalidated);
        assertTrue(cacheEngine.contains(1L));
    }

    @Test
    void testCacheStats() {
        cacheEngine.put(1L, "value1", 1L);
        cacheEngine.put(2L, "value2", 1L);

        var stats = cacheEngine.getStats();

        assertEquals(2L, stats.get("totalEntries"));
        assertNotNull(stats.get("failureRate"));
        assertNotNull(stats.get("networkDelayMs"));
        assertNotNull(stats.get("defaultTtlSeconds"));
    }

    @Test
    void testVersionUpdate() {
        cacheEngine.put(1L, "value-v1", 1L);

        Optional<CacheRecord> v1 = cacheEngine.get(1L);
        assertTrue(v1.isPresent());
        assertEquals(1L, v1.get().getVersion());

        cacheEngine.put(1L, "value-v2", 2L);

        Optional<CacheRecord> v2 = cacheEngine.get(1L);
        assertTrue(v2.isPresent());
        assertEquals(2L, v2.get().getVersion());
        assertEquals("value-v2", v2.get().getValue());
    }
}
