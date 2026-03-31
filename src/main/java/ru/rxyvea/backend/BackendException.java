package ru.rxyvea.backend;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.rxyvea.backend.api.v1.model.ErrorResponse;
import ru.rxyvea.backend.api.v1.model.ValidationError;
import ru.rxyvea.backend.service.exceptions.UserAlreadyExistsWithFieldException;

import java.util.List;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor
public enum BackendException {
    USER_ALREADY_EXISTS(
            "A User with this '%s' already exists",
            (e, detail) -> String.format(detail, ((UserAlreadyExistsWithFieldException) e).getField()),
            HttpStatus.CONFLICT
    ),
    BAD_CREDENTIALS(
            "Username and/or password is incorrect",
            null,
            HttpStatus.UNAUTHORIZED
    ),
    BAD_REQUEST(
            "Request syntax is malformed, refer to 'validation_errors'",
            null,
            HttpStatus.BAD_REQUEST
    ),
    BAD_REQUEST_METHOD(
            "Request method '%s' is not supported",
            (e, detail) -> String.format(detail, ((HttpRequestMethodNotSupportedException) e).getMethod()),
            HttpStatus.METHOD_NOT_ALLOWED
    ),
    /* Refresh token is revoked/expired. */
    INVALID_REFRESH_TOKEN(
            "Not valid refresh token provided",
            null,
            HttpStatus.BAD_REQUEST
    ),
    INVALID_TOKEN(
            "Token malformed or expired",
            null,
            HttpStatus.BAD_REQUEST
    ),
    ACCESS_DENIED(
            "Not authorized or insufficient rights",
            null,
            HttpStatus.UNAUTHORIZED
    ),
    ITEM_NOT_FOUND(
            "Requested item not found",
            null,
            HttpStatus.NOT_FOUND
    ),
    NOT_FOUND(
            "No resource '/%s'",
            (e, detail) -> String.format(detail, ((NoResourceFoundException) e).getResourcePath()),
            HttpStatus.NOT_FOUND
    );

    private final String detail;

    @Nullable
    private final BiFunction<Exception, String, String> formatter;
    private final HttpStatus status;

    public String getDetail(Exception e) {
        return (formatter != null) ? formatter.apply(e, detail) : detail;
    }

    private ResponseEntity<ErrorResponse> buildResponse(String computed, @Nullable List<ValidationError> errors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .detail(String.format("%s: %s", this.name(), computed))
                        .validation_errors(errors)
                        .build());
    }

    public ResponseEntity<ErrorResponse> issueResponseEntity() {
        return buildResponse(getDetail(), null);
    }

    public ResponseEntity<ErrorResponse> issueResponseEntity(Exception e) {
        return buildResponse(getDetail(e), null);
    }

    public ResponseEntity<ErrorResponse> issueValidationResponseEntity(List<ValidationError> errors) {
        return buildResponse(getDetail(), errors);
    }
}

