package com.wedding.management.common.api;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error payload returned by every endpoint.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors,
        Map<String, Object> details
) {
}
