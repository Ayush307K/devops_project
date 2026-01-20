package com.devops.cache.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new data record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecordRequest {

    @NotBlank(message = "Value cannot be blank")
    private String value;

    @Builder.Default
    private boolean cacheImmediately = true;
}
