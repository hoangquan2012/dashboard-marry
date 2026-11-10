package com.wedding.management.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Business rule violation. Carries an HTTP status (400 by default, 409 for conflicts)
 * and an optional machine-readable code + details map so the frontend can react
 * (e.g. show a "force" confirmation when a table is over capacity).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, "BUSINESS_RULE", message, null);
    }

    public BusinessException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message, null);
    }

    public BusinessException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public static BusinessException conflict(String code, String message, Map<String, Object> details) {
        return new BusinessException(HttpStatus.CONFLICT, code, message, details);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "CONFLICT", message, null);
    }
}
