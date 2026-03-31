package ru.rxyvea.backend.api.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Base response")
public class SuccessResponse extends BaseResponse {
    private final boolean ok = true;
    private final String detail = null;
}

