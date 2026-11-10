package com.wedding.management.security;

import com.wedding.management.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Writes the same JSON error payload as {@link com.wedding.management.common.GlobalExceptionHandler}
 * for failures that happen inside the security filter chain (before any controller).
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Bạn cần đăng nhập để truy cập tài nguyên này");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code, message,
                request.getRequestURI(), null, null);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
