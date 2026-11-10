package com.wedding.management.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI weddingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wedding Management API")
                        .description("REST API của hệ thống quản lý và thống kê đám cưới. "
                                + "Đăng nhập tại POST /api/auth/login để lấy JWT, sau đó bấm Authorize.")
                        .version("1.0.0")
                        .contact(new Contact().name("Wedding Management")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
