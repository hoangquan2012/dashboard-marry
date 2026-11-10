package com.wedding.management.web;

import com.wedding.management.dto.AuthDtos.ChangePasswordRequest;
import com.wedding.management.dto.AuthDtos.CreateUserRequest;
import com.wedding.management.dto.AuthDtos.LoginRequest;
import com.wedding.management.dto.AuthDtos.LoginResponse;
import com.wedding.management.dto.AuthDtos.ResetPasswordRequest;
import com.wedding.management.dto.AuthDtos.UpdateUserRequest;
import com.wedding.management.dto.AuthDtos.UserResponse;
import com.wedding.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Auth & Users", description = "Đăng nhập, tài khoản người dùng")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    @Operation(summary = "Đăng nhập và nhận JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/auth/me")
    @Operation(summary = "Thông tin tài khoản đang đăng nhập")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }

    @PostMapping("/auth/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đổi mật khẩu tài khoản hiện tại")
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách tài khoản (ADMIN)")
    public List<UserResponse> users() {
        return authService.listUsers();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo tài khoản mới (ADMIN)")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return authService.createUser(request);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật tài khoản (ADMIN)")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                                   Authentication authentication) {
        return authService.updateUser(id, request, authentication.getName());
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đặt lại mật khẩu cho tài khoản (ADMIN)")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(id, request);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa tài khoản (ADMIN)")
    public void deleteUser(@PathVariable Long id, Authentication authentication) {
        authService.deleteUser(id, authentication.getName());
    }
}
