package edu.uet.travel_hub.interfaces.dto.response;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorResponse> errors) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now().toString(), status, error, message, path, List.of());
    }

    public static ApiErrorResponse of(int status, String error, String message, String path,
            List<FieldErrorResponse> errors) {
        return new ApiErrorResponse(Instant.now().toString(), status, error, message, path, errors);
    }

    public record FieldErrorResponse(String field, String message) {
    }
}
