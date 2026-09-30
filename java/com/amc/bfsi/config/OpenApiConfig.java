package com.amc.bfsi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation: /swagger-ui.html and /v3/api-docs (only where springdoc is enabled - dev and h2).
 * Log in via POST /api/auth/login, paste the token into "Authorize" and try the endpoints.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "AMC Bank API", version = "1.0",
                description = "Multi-tenant banking back office: customers, accounts and loans per bank."),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
