package com.snakeforged.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Canonical error response shape returned by all API error paths.
 * {@code fieldErrors} is omitted from serialization when null (non-validation errors).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String timestamp,
        int status,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {

    public record FieldErrorDetail(String field, String message) {}

    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status, message, path, null);
    }

    public static ErrorResponse of(int status, String message, String path,
                                   List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(Instant.now().toString(), status, message, path, fieldErrors);
    }
}
