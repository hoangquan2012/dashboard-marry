package com.wedding.management.security;

import com.wedding.management.config.AppProperties;
import com.wedding.management.domain.entity.User;
import com.wedding.management.domain.enums.UserRole;
import com.wedding.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Creates the first administrator account when the users table is empty.
 * Credentials come from environment variables (APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD).
 * If no password is configured, a random one is generated and printed once in the logs.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        AppProperties.Admin admin = properties.getAdmin();
        String username = (admin.getUsername() == null || admin.getUsername().isBlank()) ? "admin" : admin.getUsername().trim();
        String password = admin.getPassword();
        boolean generated = false;
        if (password == null || password.isBlank()) {
            password = randomPassword(14);
            generated = true;
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(admin.getFullName() == null || admin.getFullName().isBlank() ? "Quản trị viên" : admin.getFullName());
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        userRepository.save(user);

        if (generated) {
            log.warn("""

                    ==========================================================================
                      Initial administrator account created
                      Username : {}
                      Password : {}
                      (APP_ADMIN_PASSWORD was not set - this password is shown only once.
                       Change it after the first login in Cài đặt > Tài khoản.)
                    ==========================================================================
                    """, username, password);
        } else {
            log.info("Initial administrator account '{}' created from environment configuration", username);
        }
    }

    private static String randomPassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
