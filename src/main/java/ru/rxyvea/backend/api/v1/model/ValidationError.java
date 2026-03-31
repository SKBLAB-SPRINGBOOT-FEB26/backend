package ru.rxyvea.backend.api.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Validation error")
public class ValidationError {
    @Schema(description = "Validation error field")
    private final String field;

    @Schema(description = "Validation error details")
    private final String detail;
}
