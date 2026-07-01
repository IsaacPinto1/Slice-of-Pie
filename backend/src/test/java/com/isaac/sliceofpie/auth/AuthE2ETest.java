package com.isaac.sliceofpie.auth;

import com.isaac.sliceofpie.auth.AuthDtos.LoginResponse;
import com.isaac.sliceofpie.auth.AuthDtos.RegisterResponse;
import com.isaac.sliceofpie.users.UserDtos.MeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthE2ETest {

    private WebTestClient client;

    @BeforeEach
    void setup(@LocalServerPort int port) {
        client = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void full_auth_flow() {

        String username = AuthTestUtils.uniqueUsername();

        // REGISTER
        RegisterResponse registerRes = client.post()
                .uri("/auth/register")
                .bodyValue(Map.of("username", username, "password", "1234"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RegisterResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(registerRes);

        // LOGIN
        LoginResponse loginRes = client.post()
                .uri("/auth/login")
                .bodyValue(Map.of("username", username, "password", "1234"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(loginRes);
        assertNotNull(loginRes.token());

        String token = loginRes.token();

        // /me
        MeResponse meRes = client.get()
                .uri("/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MeResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(meRes);
        assertEquals(username, meRes.username());
    }
}