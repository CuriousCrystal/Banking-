package com.amc.bfsi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full HTTP stack on a random port with H2 - proves the security chain end to end:
 * public health, JWT issue, 401 without token, 403 for the wrong role.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiSecurityTest {

    @Autowired TestRestTemplate rest;

    @BeforeEach
    void useJdkHttpClient() {
        // the default HttpURLConnection-based factory chokes on 401 responses to
        // POST-with-body ("cannot retry due to server authentication"); the JDK HttpClient does not
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void healthIsPublic() {
        ResponseEntity<Map> res = rest.getForEntity("/api/health", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsEntry("status", "UP");
    }

    @Test
    void customersWithoutTokenIs401() {
        ResponseEntity<String> res = rest.getForEntity("/api/customers", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void badPasswordIs401() {
        ResponseEntity<Map> res = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "wrong"), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminLoginIssuesTokenAndCanReadCustomers() {
        ResponseEntity<Map> login = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "admin123"), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) login.getBody().get("token");
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> customers = rest.exchange(
                "/api/customers", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(customers.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customers.getBody()).contains("email");
    }

    @Test
    void clerkCannotDelete() {
        String token = login("clerk", "clerk123");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> res = rest.exchange(
                "/api/customers/1", HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void emiEndpointNeedsAuth() {
        ResponseEntity<String> res = rest.getForEntity("/api/loans/1/emi", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String login(String user, String pass) {
        ResponseEntity<Map> login = rest.postForEntity("/api/auth/login",
                Map.of("username", user, "password", pass), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) login.getBody().get("token");
    }
}
