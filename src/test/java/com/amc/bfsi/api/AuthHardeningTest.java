package com.amc.bfsi.api;

import com.amc.bfsi.entity.Tenant;
import com.amc.bfsi.repository.TenantRepository;
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

/** Login, token and error-handling behaviour that a public SaaS endpoint has to get right. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class AuthHardeningTest {

    @Autowired TestRestTemplate rest;
    @Autowired TenantRepository tenants;
    ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(rest);
    }

    @Test
    void meNeedsAToken() {
        // it used to sit under the public /api/auth/** rule; it must only answer to a signed-in user
        assertThat(api.call(null, HttpMethod.GET, "/api/auth/me", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meShowsTheUserAndTheirBank() {
        String code = ApiClient.uniqueCode("me");
        String token = api.signupToken(code);

        Map<String, Object> me = map(api.call(token, HttpMethod.GET, "/api/auth/me", null).getBody());

        assertThat(me).containsEntry("username", "admin");
        assertThat(map(me.get("tenant"))).containsEntry("id", code);
    }

    @Test
    void wrongPasswordUnknownBankAndForeignBankAreAllTheSame401() {
        String code = ApiClient.uniqueCode("login");
        String token = api.signupToken(code);
        String otherCode = ApiClient.uniqueCode("other");
        api.signupToken(otherCode);

        assertThat(api.login(code, "admin", ApiClient.PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.login(code, "admin", "wrong-password").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.login("no-such-bank", "admin", ApiClient.PASSWORD).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // a user that exists only in the first bank cannot sign in through the other one
        api.call(token, HttpMethod.POST, "/api/users",
                Map.of("username", "onlyhere", "password", "only-here1", "role", "USER"));
        assertThat(api.login(code, "onlyhere", "only-here1").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.login(otherCode, "onlyhere", "only-here1").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void suspendedBankIsLockedOutImmediately() {
        String code = ApiClient.uniqueCode("susp");
        String token = api.signupToken(code);
        assertThat(api.call(token, HttpMethod.GET, "/api/customers", null).getStatusCode()).isEqualTo(HttpStatus.OK);

        Tenant tenant = tenants.findById(code).orElseThrow();
        tenant.setStatus(Tenant.Status.SUSPENDED);
        tenants.save(tenant);

        // the token that worked a moment ago is worthless now, and nobody can log in
        assertThat(api.call(token, HttpMethod.GET, "/api/customers", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.login(code, "admin", ApiClient.PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void clientMistakesAreNot500s() {
        String token = api.signupToken(ApiClient.uniqueCode("err"));

        ResponseEntity<Object> unknownUrl = api.call(token, HttpMethod.GET, "/api/no-such-thing", null);
        assertThat(unknownUrl.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Object> badId = api.call(token, HttpMethod.GET, "/api/customers/abc", null);
        assertThat(badId.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
