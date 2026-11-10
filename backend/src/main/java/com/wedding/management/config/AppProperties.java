package com.wedding.management.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String timezone = "Asia/Ho_Chi_Minh";
    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Storage storage = new Storage();
    private Cors cors = new Cors();
    private Seed seed = new Seed();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        private long expirationMinutes = 720;
        private String issuer = "wedding-management";
    }

    @Getter
    @Setter
    public static class Admin {
        private String username = "admin";
        private String password = "";
        private String fullName = "Quản trị viên";
    }

    @Getter
    @Setter
    public static class Storage {
        private String uploadDir = "./uploads";
        private long maxFileSizeBytes = 10L * 1024 * 1024;
        private List<String> allowedContentTypes = new ArrayList<>(List.of(
                "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf"));
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:4200"));
    }

    @Getter
    @Setter
    public static class Seed {
        private boolean demoData = false;
    }
}
