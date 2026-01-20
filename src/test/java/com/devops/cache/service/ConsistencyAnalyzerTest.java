package com.devops.cache.service;

import com.devops.cache.dto.DriftReport;
import com.devops.cache.model.CacheRecord;
import com.devops.cache.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsistencyAnalyzer service.
 */
@ExtendWith(MockitoExtension.class)
class ConsistencyAnalyzerTest {

    @Mock
    private DBEngine dbEngine;

    @Mock
    private CacheEngine cacheEngine;

    @InjectMocks
    private ConsistencyAnalyzer consistencyAnalyzer;

    private DataRecord dbRecord1;
    private DataRecord dbRecord2;
    private CacheRecord cacheRecord1Stale;
    private CacheRecord cacheRecord2Fresh;

    @BeforeEach
    void setUp() {
        dbRecord1 = DataRecord.builder()
                .id(1L)
                .value("value1")
                .version(5L)
                .lastUpdated(LocalDateTime.now())
                .build();

        dbRecord2 = DataRecord.builder()
                .id(2L)
                .value("value2")
                .version(3L)
                .lastUpdated(LocalDateTime.now())
                .build();

        cacheRecord1Stale = CacheRecord.builder()
                .id(1L)
                .value("old-value1")
                .version(3L)
                .cachedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        cacheRecord2Fresh = CacheRecord.builder()
                .id(2L)
                .value("value2")
                .version(3L)
                .cachedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testAnalyzeDrift_WithStaleData() {
        when(dbEngine.getAllRecords()).thenReturn(Arrays.asList(dbRecord1, dbRecord2));

        Map<Long, CacheRecord> cachedRecords = new HashMap<>();
        cachedRecords.put(1L, cacheRecord1Stale);
        cachedRecords.put(2L, cacheRecord2Fresh);
        when(cacheEngine.getAllCached()).thenReturn(cachedRecords);

        when(cacheEngine.get(1L)).thenReturn(Optional.of(cacheRecord1Stale));
        when(cacheEngine.get(2L)).thenReturn(Optional.of(cacheRecord2Fresh));

        DriftReport report = consistencyAnalyzer.analyzeDrift(false);

        assertNotNull(report);
        assertEquals(2L, report.getTotalRecords());
        assertEquals(1L, report.getStaleRecords());
        assertEquals(50.0, report.getDriftScore(), 0.01);
        assertEquals(DriftReport.SystemVerdict.RISK, report.getVerdict());
    }

    @Test
    void testAnalyzeDrift_NoStaleData() {
        DataRecord freshDbRecord = DataRecord.builder()
                .id(1L)
                .value("value1")
                .version(3L)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(dbEngine.getAllRecords()).thenReturn(Arrays.asList(freshDbRecord));

        Map<Long, CacheRecord> cachedRecords = new HashMap<>();
        cachedRecords.put(1L, cacheRecord2Fresh.toBuilder().id(1L).build());
        when(cacheEngine.getAllCached()).thenReturn(cachedRecords);

        when(cacheEngine.get(1L)).thenReturn(Optional.of(cacheRecord2Fresh.toBuilder().id(1L).build()));

        DriftReport report = consistencyAnalyzer.analyzeDrift(false);

        assertNotNull(report);
        assertEquals(0L, report.getStaleRecords());
        assertEquals(0.0, report.getDriftScore());
        assertEquals(DriftReport.SystemVerdict.HEALTHY, report.getVerdict());
    }

    @Test
    void testAnalyzeDrift_WithAutoFix() {
        when(dbEngine.getAllRecords()).thenReturn(Arrays.asList(dbRecord1));

        Map<Long, CacheRecord> cachedRecords = new HashMap<>();
        cachedRecords.put(1L, cacheRecord1Stale);
        when(cacheEngine.getAllCached()).thenReturn(cachedRecords);

        when(cacheEngine.get(1L)).thenReturn(Optional.of(cacheRecord1Stale));

        DriftReport report = consistencyAnalyzer.analyzeDrift(true);

        verify(cacheEngine, times(1)).put(1L, dbRecord1.getValue(), dbRecord1.getVersion());
        assertEquals(1L, report.getAutoFixedCount());
    }

    @Test
    void testIsRecordStale_True() {
        when(dbEngine.getRecord(1L)).thenReturn(Optional.of(dbRecord1));
        when(cacheEngine.get(1L)).thenReturn(Optional.of(cacheRecord1Stale));

        boolean isStale = consistencyAnalyzer.isRecordStale(1L);

        assertTrue(isStale);
    }

    @Test
    void testIsRecordStale_False() {
        when(dbEngine.getRecord(2L)).thenReturn(Optional.of(dbRecord2));
        when(cacheEngine.get(2L)).thenReturn(Optional.of(cacheRecord2Fresh));

        boolean isStale = consistencyAnalyzer.isRecordStale(2L);

        assertFalse(isStale);
    }

    @Test
    void testIsRecordStale_NotCached() {
        when(dbEngine.getRecord(1L)).thenReturn(Optional.of(dbRecord1));
        when(cacheEngine.get(1L)).thenReturn(Optional.empty());

        boolean isStale = consistencyAnalyzer.isRecordStale(1L);

        assertFalse(isStale);
    }

    @Test
    void testForceRefresh() {
        when(dbEngine.getRecord(1L)).thenReturn(Optional.of(dbRecord1));

        consistencyAnalyzer.forceRefresh(1L);

        verify(cacheEngine, times(1)).put(1L, dbRecord1.getValue(), dbRecord1.getVersion());
    }

    @Test
    void testForceRefresh_RecordNotFound() {
        when(dbEngine.getRecord(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            consistencyAnalyzer.forceRefresh(999L);
        });
    }

    @Test
    void testDriftScoreCalculation() {
        when(dbEngine.getAllRecords()).thenReturn(Arrays.asList(dbRecord1, dbRecord2));

        Map<Long, CacheRecord> cachedRecords = new HashMap<>();
        cachedRecords.put(1L, cacheRecord1Stale);
        cachedRecords.put(2L, cacheRecord2Fresh);
        when(cacheEngine.getAllCached()).thenReturn(cachedRecords);

        when(cacheEngine.get(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.ofNullable(cachedRecords.get(id));
        });

        DriftReport report = consistencyAnalyzer.analyzeDrift(false);

        double expectedDriftScore = (1.0 / 2.0) * 100;
        assertEquals(expectedDriftScore, report.getDriftScore(), 0.01);
    }
}
