package ru.rxyvea.backend.api.v1.auth.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        List<String> roles
) {
}
