package com.amc.bfsi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Password guessing is throttled per client IP - its own context, with a tiny limit. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.auth.rate-limit-per-minute=5")
@ActiveProfiles("h2")
class RateLimitTest {

    @Autowired TestRestTemplate rest;

    @Test
    void theSixthLoginAttemptInAMinuteIsRefused() {
        ApiClient api = new ApiClient(rest);

        for (int i = 0; i < 5; i++) {
            assertThat(api.login("amc-bank", "admin", "wrong-" + i).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // even the correct password is refused once the limit is hit - that is what stops guessing
        ResponseEntity<Object> refused = api.login("amc-bank", "admin", "admin123");
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(refused.getHeaders().getFirst("Retry-After")).isEqualTo("60");
    }
}
