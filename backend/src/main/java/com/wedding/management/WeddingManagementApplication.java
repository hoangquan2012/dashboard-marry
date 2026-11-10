package com.wedding.management;

import com.wedding.management.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class WeddingManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeddingManagementApplication.class, args);
    }

    @PostConstruct
    void configureTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
}
