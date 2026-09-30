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

import static com.amc.bfsi.api.ApiClient.list;
import static com.amc.bfsi.api.ApiClient.map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The SaaS promise: one bank can never see or touch another bank's data - not in lists,
 * not by guessing an id, not through a related resource.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class TenantIsolationTest {

    @Autowired TestRestTemplate rest;
    ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(rest);
    }

    private HttpStatus status(String token, HttpMethod method, String path, Object body) {
        return HttpStatus.valueOf(api.call(token, method, path, body).getStatusCode().value());
    }

    @Test
    void bankBCannotSeeOrTouchBankAData() {
        String a = api.signupToken(ApiClient.uniqueCode("iso-a"));
        String b = api.signupToken(ApiClient.uniqueCode("iso-b"));

        Object customerId = api.createCustomer(a, "alice@example.com");

        // an account and a loan in bank A
        ResponseEntity<Object> account = api.call(a, HttpMethod.POST, "/api/accounts",
                Map.of("customerId", customerId, "accountType", "CURRENT", "balance", 150000));
        assertThat(account.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Object accountId = map(account.getBody()).get("accountId");
        ResponseEntity<Object> loan = api.call(a, HttpMethod.POST, "/api/loans", Map.of(
                "customerId", customerId, "loanType", "HOME", "principalAmount", 100000,
                "interestRate", 8.5, "tenureMonths", 60));
        assertThat(loan.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Object loanId = map(loan.getBody()).get("loanId");

        // bank B: lists are empty ...
        assertThat(list(api.call(b, HttpMethod.GET, "/api/customers", null).getBody())).isEmpty();
        assertThat(list(api.call(b, HttpMethod.GET, "/api/accounts", null).getBody())).isEmpty();
        assertThat(list(api.call(b, HttpMethod.GET, "/api/loans", null).getBody())).isEmpty();
        assertThat(map(api.call(b, HttpMethod.GET, "/api/reports/summary", null).getBody()).get("customers"))
                .isEqualTo(0);

        // ... and guessing ids gets 404, never the data (read, change, delete, or through a relation)
        assertThat(status(b, HttpMethod.GET, "/api/customers/" + customerId, null)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(b, HttpMethod.PUT, "/api/customers/" + customerId, Map.of(
                "name", "Mallory", "email", "m@example.com", "city", "Pune", "panNumber", "ABCDE1234F")))
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(b, HttpMethod.DELETE, "/api/customers/" + customerId, null)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(b, HttpMethod.GET, "/api/accounts/" + accountId, null)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(b, HttpMethod.PATCH, "/api/accounts/" + accountId + "/withdraw", Map.of("amount", 1)))
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(b, HttpMethod.POST, "/api/accounts", Map.of(
                "customerId", customerId, "accountType", "CURRENT", "balance", 0))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(b, HttpMethod.GET, "/api/loans/" + loanId + "/emi", null)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(b, HttpMethod.GET, "/api/loans/customer/" + customerId, null)).isEqualTo(HttpStatus.NOT_FOUND);

        // bank A is untouched by all of that
        assertThat(status(a, HttpMethod.GET, "/api/customers/" + customerId, null)).isEqualTo(HttpStatus.OK);
        Object balance = map(api.call(a, HttpMethod.GET, "/api/accounts/" + accountId, null).getBody()).get("balance");
        assertThat(((Number) balance).doubleValue()).isEqualTo(150000.0);
    }

    @Test
    void sameEmailAndUsernameAreFineInDifferentBanks() {
        String a = api.signupToken(ApiClient.uniqueCode("dup-a"));   // both banks get an admin called "admin"
        String b = api.signupToken(ApiClient.uniqueCode("dup-b"));

        api.createCustomer(a, "same@example.com");
        api.createCustomer(b, "same@example.com");                    // not a duplicate: a different bank

        assertThat(list(api.call(a, HttpMethod.GET, "/api/customers", null).getBody())).hasSize(1);
        assertThat(list(api.call(b, HttpMethod.GET, "/api/customers", null).getBody())).hasSize(1);
    }

    @Test
    void usersAreScopedToTheirBank() {
        String a = api.signupToken(ApiClient.uniqueCode("usr-a"));
        String b = api.signupToken(ApiClient.uniqueCode("usr-b"));

        Map<String, Object> clerk = Map.of("username", "clerk1", "password", "clerk-pass1", "role", "USER");
        assertThat(status(a, HttpMethod.POST, "/api/users", clerk)).isEqualTo(HttpStatus.CREATED);

        assertThat(list(api.call(a, HttpMethod.GET, "/api/users", null).getBody())).hasSize(2);   // admin + clerk1
        assertThat(list(api.call(b, HttpMethod.GET, "/api/users", null).getBody())).hasSize(1);   // only its own admin
        assertThat(status(b, HttpMethod.POST, "/api/users", clerk)).isEqualTo(HttpStatus.CREATED); // name is free in B
    }
}
