package com.wedding.management.dto;

import com.wedding.management.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Vui lòng nhập tên đăng nhập") @Size(max = 50) String username,
            @NotBlank(message = "Vui lòng nhập mật khẩu") @Size(max = 100) String password
    ) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
    }

    public record UserResponse(Long id, String username, String fullName, UserRole role, boolean enabled,
                               Instant lastLoginAt, Instant createdAt) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @Size(min = 8, max = 100, message = "Mật khẩu mới phải có ít nhất 8 ký tự") String newPassword
    ) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "Vui lòng nhập tên đăng nhập")
            @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự") String username,
            @NotBlank(message = "Vui lòng nhập mật khẩu")
            @Size(min = 8, max = 100, message = "Mật khẩu phải có ít nhất 8 ký tự") String password,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150) String fullName,
            UserRole role
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150) String fullName,
            UserRole role,
            Boolean enabled
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @Size(min = 8, max = 100, message = "Mật khẩu phải có ít nhất 8 ký tự") String newPassword
    ) {
    }
}
