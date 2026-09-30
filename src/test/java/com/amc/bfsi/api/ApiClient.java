package com.amc.bfsi.api;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tiny helper so the HTTP tests read like a script: sign a bank up, then call the API as its admin. */
class ApiClient {

    static final String PASSWORD = "password123";

    private final TestRestTemplate rest;

    ApiClient(TestRestTemplate rest) {
        // the default HttpURLConnection-based factory chokes on 401 responses to POST-with-body and has no PATCH
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        this.rest = rest;
    }

    /** A bank code that is valid and unique for this test run (all tests share one in-memory database). */
    static String uniqueCode(String prefix) {
        return prefix + "-" + Long.toString(System.nanoTime(), 36);
    }

    ResponseEntity<Object> signup(String code, String username, String password) {
        return call(null, HttpMethod.POST, "/api/auth/signup", Map.of(
                "bankName", "Bank " + code, "bankCode", code, "username", username, "password", password));
    }

    /** Signs a new bank up (first admin is "admin") and returns that admin's token. */
    String signupToken(String code) {
        ResponseEntity<Object> res = signup(code, "admin", PASSWORD);
        if (res.getStatusCode() != HttpStatus.CREATED) {
            throw new AssertionError("signup of " + code + " failed: " + res.getStatusCode() + " " + res.getBody());
        }
        return (String) map(res.getBody()).get("token");
    }

    ResponseEntity<Object> login(String code, String username, String password) {
        Map<String, Object> body = new HashMap<>();
        if (code != null) {
            body.put("tenant", code);
        }
        body.put("username", username);
        body.put("password", password);
        return call(null, HttpMethod.POST, "/api/auth/login", body);
    }

    ResponseEntity<Object> call(String token, HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), Object.class);
    }

    /** Creates a customer in the bank behind the token and returns its id. */
    Object createCustomer(String token, String email) {
        ResponseEntity<Object> res = call(token, HttpMethod.POST, "/api/customers", Map.of(
                "name", "Test Customer", "email", email, "city", "Pune", "panNumber", "ABCDE1234F"));
        if (res.getStatusCode() != HttpStatus.CREATED) {
            throw new AssertionError("customer create failed: " + res.getStatusCode() + " " + res.getBody());
        }
        return map(res.getBody()).get("customerId");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object body) {
        return (Map<String, Object>) body;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object body) {
        return (List<Object>) body;
    }
}
