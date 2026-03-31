package ru.rxyvea.backend.api.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@Schema(description = "Base response")
public class BaseResponse {
    @Schema(description = "Response status")
    private final boolean ok;

    @Nullable
    @Schema(description = "Response error details if any")
    private final String detail;

    @Nullable
    @Schema(description = "Validation errors if any")
    private final List<ValidationError> validation_errors;
}
