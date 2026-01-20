package com.devops.cache.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing data record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecordRequest {

    @NotBlank(message = "Value cannot be blank")
    private String value;

    @Builder.Default
    private boolean invalidateCache = true;

    @Builder.Default
    private boolean simulateFailure = false;
}
