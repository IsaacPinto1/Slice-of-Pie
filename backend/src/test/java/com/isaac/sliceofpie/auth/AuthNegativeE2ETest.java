package com.isaac.sliceofpie.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthNegativeE2ETest {

    private WebTestClient client;

    @BeforeEach
    void setup(@LocalServerPort int port) {
        client = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void wrong_password_fails_login() {

        String username = AuthTestUtils.uniqueUsername();

        client.post()
                .uri("/auth/register")
                .bodyValue(Map.of("username", username, "password", "1234"))
                .exchange()
                .expectStatus().isCreated();

        client.post()
                .uri("/auth/login")
                .bodyValue(Map.of("username", username, "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void missing_token_fails_me() {

        client.get()
                .uri("/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void invalid_token_fails_me() {

        client.get()
                .uri("/me")
                .header("Authorization", "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}