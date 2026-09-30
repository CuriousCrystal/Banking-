package com.amc.bfsi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static com.amc.bfsi.api.ApiClient.map;
import static org.assertj.core.api.Assertions.assertThat;

/** Self-service onboarding and the subscription-plan limits behind it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class SignupAndPlanTest {

    @Autowired TestRestTemplate rest;
    ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(rest);
    }

    @Test
    void signupCreatesABankAndSignsItsAdminIn() {
        String code = ApiClient.uniqueCode("new");
        ResponseEntity<Object> res = api.signup(code, "owner", "strong-pass1");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = map(res.getBody());
        String token = (String) body.get("token");
        assertThat(token).isNotBlank();
        assertThat(ApiClient.list(body.get("roles"))).containsExactly("ROLE_ADMIN");
        assertThat(map(body.get("tenant"))).containsEntry("id", code).containsEntry("plan", "FREE");

        // the new admin is already signed in, and sees the plan and how much of it is used
        Map<String, Object> tenant = map(api.call(token, HttpMethod.GET, "/api/tenant", null).getBody());
        assertThat(tenant).containsEntry("plan", "FREE")
                .containsEntry("maxCustomers", 25).containsEntry("maxUsers", 3)
                .containsEntry("customers", 0).containsEntry("users", 1);
    }

    @Test
    void aBankCodeCanOnlyBeTakenOnce() {
        String code = ApiClient.uniqueCode("taken");
        api.signupToken(code);

        assertThat(api.signup(code, "other", "strong-pass1").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        // codes that would look official or clash with a route are reserved
        assertThat(api.signup("admin", "other", "strong-pass1").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void badSignupInputIs400WithFieldErrors() {
        ResponseEntity<Object> res = api.signup("Bad Code!", "x", "short");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(map(map(res.getBody()).get("errors"))).containsKeys("bankCode", "username", "password");
    }

    @Test
    void freePlanStopsAtItsCustomerLimit() {
        String token = api.signupToken(ApiClient.uniqueCode("cap"));
        for (int i = 0; i < 25; i++) {
            api.createCustomer(token, "c" + i + "@example.com");
        }

        ResponseEntity<Object> res = api.call(token, HttpMethod.POST, "/api/customers", Map.of(
                "name", "One Too Many", "email", "extra@example.com", "city", "Pune", "panNumber", "ABCDE1234F"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat((String) map(res.getBody()).get("message")).contains("FREE").contains("25");
    }

    @Test
    void freePlanStopsAtItsUserLimit() {
        String token = api.signupToken(ApiClient.uniqueCode("usr"));   // the admin is user 1 of 3

        for (int i = 1; i <= 2; i++) {
            assertThat(api.call(token, HttpMethod.POST, "/api/users",
                    Map.of("username", "user" + i, "password", "user-pass" + i, "role", "USER")).getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);
        }
        assertThat(api.call(token, HttpMethod.POST, "/api/users",
                Map.of("username", "user3", "password", "user-pass3", "role", "USER")).getStatusCode())
                .isEqualTo(HttpStatus.PAYMENT_REQUIRED);
    }
}
