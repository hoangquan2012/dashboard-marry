package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.User;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.UserRole;
import com.wedding.management.dto.AuthDtos.ChangePasswordRequest;
import com.wedding.management.dto.AuthDtos.CreateUserRequest;
import com.wedding.management.dto.AuthDtos.LoginRequest;
import com.wedding.management.dto.AuthDtos.LoginResponse;
import com.wedding.management.dto.AuthDtos.ResetPasswordRequest;
import com.wedding.management.dto.AuthDtos.UpdateUserRequest;
import com.wedding.management.dto.AuthDtos.UserResponse;
import com.wedding.management.repository.UserRepository;
import com.wedding.management.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        User user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
        user.setLastLoginAt(Instant.now());
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getFullName());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(), toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        return toResponse(getByUsername(username));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = getByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.log("USER", user.getId(), user.getUsername(), AuditAction.UPDATE, "Đổi mật khẩu");
    }

    // ---- user administration -------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw BusinessException.conflict("Tên đăng nhập đã tồn tại");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role() == null ? UserRole.STAFF : request.role());
        user.setEnabled(true);
        userRepository.save(user);
        auditService.log("USER", user.getId(), user.getUsername(), AuditAction.CREATE, "Tạo tài khoản " + user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String actor) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tài khoản", id));
        if (user.getUsername().equalsIgnoreCase(actor)) {
            if (request.enabled() != null && !request.enabled()) {
                throw new BusinessException("Không thể tự vô hiệu hóa tài khoản đang đăng nhập");
            }
            if (request.role() != null && request.role() != UserRole.ADMIN) {
                throw new BusinessException("Không thể tự hạ quyền tài khoản đang đăng nhập");
            }
        }
        user.setFullName(request.fullName().trim());
        if (request.role() != null) user.setRole(request.role());
        if (request.enabled() != null) user.setEnabled(request.enabled());
        ensureAtLeastOneActiveAdmin();
        auditService.log("USER", user.getId(), user.getUsername(), AuditAction.UPDATE, "Cập nhật tài khoản " + user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tài khoản", id));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.log("USER", user.getId(), user.getUsername(), AuditAction.UPDATE, "Đặt lại mật khẩu cho " + user.getUsername());
    }

    @Transactional
    public void deleteUser(Long id, String actor) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tài khoản", id));
        if (user.getUsername().equalsIgnoreCase(actor)) {
            throw new BusinessException("Không thể xóa tài khoản đang đăng nhập");
        }
        userRepository.delete(user);
        userRepository.flush();
        ensureAtLeastOneActiveAdmin();
        auditService.log("USER", id, user.getUsername(), AuditAction.DELETE, "Xóa tài khoản " + user.getUsername());
    }

    private void ensureAtLeastOneActiveAdmin() {
        boolean hasAdmin = userRepository.findAll().stream()
                .anyMatch(u -> u.isEnabled() && u.getRole() == UserRole.ADMIN);
        if (!hasAdmin) {
            throw new BusinessException("Hệ thống phải có ít nhất một tài khoản quản trị đang hoạt động");
        }
    }

    private User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isEnabled(),
                u.getLastLoginAt(), u.getCreatedAt());
    }
}
