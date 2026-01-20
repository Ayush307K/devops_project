package com.devops.cache.repository;

import com.devops.cache.model.DataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for DataRecord entity.
 */
@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, Long> {

    /**
     * Finds all records with a specific version.
     */
    List<DataRecord> findByVersion(Long version);

    /**
     * Counts records with version greater than specified value.
     */
    long countByVersionGreaterThan(Long version);
}
