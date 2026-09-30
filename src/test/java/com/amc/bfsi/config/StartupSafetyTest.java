package com.amc.bfsi.config;

import com.amc.bfsi.BfsiApplication;
import com.amc.bfsi.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A misconfigured or failing server must stop or stay quiet - never run insecurely or leak internals. */
class StartupSafetyTest {

    @Test
    void productionRefusesToStartWithoutItsSecrets() {
        // --spring.profiles.active beats the h2 default that surefire sets, so this really is "prod"
        assertThatThrownBy(() -> new SpringApplicationBuilder(BfsiApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=prod"))
                .hasStackTraceContaining("Missing required environment variables")
                .hasStackTraceContaining("JWT_SECRET");
    }

    @Test
    void aTooShortJwtSecretStopsTheStartup() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(BfsiApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=h2", "--app.jwt.secret=c2hvcnQ="))     // "short"
                .hasStackTraceContaining("not secure enough");
    }

    @Test
    void unexpectedErrorsDoNotLeakTheirMessage() {
        var response = new GlobalExceptionHandler()
                .everythingElse(new RuntimeException("select * from app_user where password = 'x'"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        String message = String.valueOf(response.getBody().get("message"));
        assertThat(message).contains("reference").doesNotContain("select");
    }
}
