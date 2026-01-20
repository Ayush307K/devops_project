package com.devops.cache.controller;

import com.devops.cache.dto.CreateRecordRequest;
import com.devops.cache.dto.UpdateRecordRequest;
import com.devops.cache.model.DataRecord;
import com.devops.cache.service.DBEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DatabaseController.
 */
@WebMvcTest(DatabaseController.class)
class DatabaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DBEngine dbEngine;

    @Test
    void testCreateRecord() throws Exception {
        CreateRecordRequest request = CreateRecordRequest.builder()
                .value("test-value")
                .cacheImmediately(true)
                .build();

        DataRecord createdRecord = DataRecord.builder()
                .id(1L)
                .value("test-value")
                .version(1L)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(dbEngine.createRecord(anyString(), anyBoolean())).thenReturn(createdRecord);

        mockMvc.perform(post("/api/db/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.value").value("test-value"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void testGetRecord() throws Exception {
        DataRecord record = DataRecord.builder()
                .id(1L)
                .value("test-value")
                .version(1L)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(dbEngine.getRecord(1L)).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/db/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.value").value("test-value"));
    }

    @Test
    void testGetRecord_NotFound() throws Exception {
        when(dbEngine.getRecord(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllRecords() throws Exception {
        DataRecord record1 = DataRecord.builder()
                .id(1L)
                .value("value1")
                .version(1L)
                .lastUpdated(LocalDateTime.now())
                .build();

        DataRecord record2 = DataRecord.builder()
                .id(2L)
                .value("value2")
                .version(1L)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(dbEngine.getAllRecords()).thenReturn(Arrays.asList(record1, record2));

        mockMvc.perform(get("/api/db/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void testUpdateRecord() throws Exception {
        UpdateRecordRequest request = UpdateRecordRequest.builder()
                .value("updated-value")
                .invalidateCache(true)
                .simulateFailure(false)
                .build();

        DataRecord updatedRecord = DataRecord.builder()
                .id(1L)
                .value("updated-value")
                .version(2L)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(dbEngine.updateRecord(eq(1L), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(updatedRecord);

        mockMvc.perform(put("/api/db/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.value").value("updated-value"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void testDeleteRecord() throws Exception {
        mockMvc.perform(delete("/api/db/1"))
                .andExpect(status().isNoContent());
    }
}
