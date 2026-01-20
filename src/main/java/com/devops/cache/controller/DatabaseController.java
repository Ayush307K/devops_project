package com.devops.cache.controller;

import com.devops.cache.dto.CreateRecordRequest;
import com.devops.cache.dto.UpdateRecordRequest;
import com.devops.cache.model.DataRecord;
import com.devops.cache.service.DBEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for database operations.
 */
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
@Slf4j
public class DatabaseController {

    private final DBEngine dbEngine;

    /**
     * Creates a new record in the database.
     */
    @PostMapping("/create")
    public ResponseEntity<DataRecord> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        log.info("Creating new record with value: {}", request.getValue());
        DataRecord record = dbEngine.createRecord(request.getValue(), request.isCacheImmediately());
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    /**
     * Updates an existing record.
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<DataRecord> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecordRequest request) {

        log.info("Updating record id: {} with new value", id);

        try {
            DataRecord updated = dbEngine.updateRecord(
                    id,
                    request.getValue(),
                    request.isInvalidateCache(),
                    request.isSimulateFailure()
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Record not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves a record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataRecord> getRecord(@PathVariable Long id) {
        log.info("Fetching record id: {}", id);
        Optional<DataRecord> record = dbEngine.getRecord(id);
        return record.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all records.
     */
    @GetMapping("/all")
    public ResponseEntity<List<DataRecord>> getAllRecords() {
        log.info("Fetching all records");
        List<DataRecord> records = dbEngine.getAllRecords();
        return ResponseEntity.ok(records);
    }

    /**
     * Deletes a record by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        log.info("Deleting record id: {}", id);
        dbEngine.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
