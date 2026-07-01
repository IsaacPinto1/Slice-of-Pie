package com.isaac.sliceofpie.auth;

import java.util.UUID;

import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

import com.isaac.sliceofpie.auth.AuthDtos.*;

public final class AuthTestUtils {

    private AuthTestUtils() {}

    public static String uniqueUsername() {
        return "user_" + UUID.randomUUID();
    }

    public static String registerAndLogin(
            WebTestClient client,
            String password) {
        
        String username = uniqueUsername();

        client.post()
                .uri("/auth/register")
                .bodyValue(new RegisterRequest(username, password))
                .exchange()
                .expectStatus().isCreated();

        LoginResponse response = client.post()
                .uri("/auth/login")
                .bodyValue(new LoginRequest(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        return response.token();
    }
    public static String registerAndLogin(
            WebTestClient client,
            String username,
            String password) {

        client.post()
                .uri("/auth/register")
                .bodyValue(new RegisterRequest(username, password))
                .exchange()
                .expectStatus().isCreated();

        LoginResponse response = client.post()
                .uri("/auth/login")
                .bodyValue(new LoginRequest(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        return response.token();
    }
}