package com.isaac.sliceofpie.thesis;

import com.isaac.sliceofpie.auth.AuthTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ThesisFlowTest {

    private WebTestClient client;

    @BeforeEach
    void setup(@LocalServerPort int port) {
        client = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void create_update_and_fetch_thesis() {

        String username = AuthTestUtils.uniqueUsername();
        String password = "1234";
        String token = AuthTestUtils.registerAndLogin(client, username, password);

        // CREATE
        client.post()
                .uri("/thesis")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of(
                        "ticker", "AAPL",
                        "content", "Long term bullish due to ecosystem lock-in"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ticker").isEqualTo("AAPL")
                .jsonPath("$.content").isEqualTo("Long term bullish due to ecosystem lock-in");

        // UPDATE (same endpoint = upsert)
        client.post()
                .uri("/thesis")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of(
                        "ticker", "AAPL",
                        "content", "Revised thesis after earnings"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isEqualTo("Revised thesis after earnings");

        // FETCH
        client.get()
                .uri("/thesis/AAPL")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ticker").isEqualTo("AAPL")
                .jsonPath("$.content").isEqualTo("Revised thesis after earnings");
    }

    @Test
    void thesis_requires_auth() {

        client.post()
                .uri("/thesis")
                .bodyValue(Map.of("ticker", "AAPL", "content", "test"))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}