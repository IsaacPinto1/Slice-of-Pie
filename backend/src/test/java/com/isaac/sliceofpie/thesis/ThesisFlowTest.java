package com.isaac.sliceofpie.thesis;

import com.isaac.sliceofpie.auth.AuthTestUtils;
import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;
import com.isaac.sliceofpie.instrument.InstrumentTestUtils;
import com.isaac.sliceofpie.instrument.lookup.InstrumentLookupClient;
import com.isaac.sliceofpie.thesis.ThesisDtos.ThesisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ThesisFlowTest {

    private WebTestClient client;

    // Only needed so InstrumentTestUtils.resolveInstrumentId can seed a real
    // instrument without hitting Finnhub. Nothing in Thesis itself resolves
    // anything - by the time a thesis exists, the instrument already does.
    @MockitoBean
    private InstrumentLookupClient instrumentLookupClient;

    private String token;
    private Long aaplId;

    @BeforeEach
    void setup(@LocalServerPort int port) {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        // Test is search agnostic
        when(instrumentLookupClient.search("AAPL")) 
                .thenReturn(List.of(new InstrumentSearchResult("AAPL", "APPLE INC")));

        token = AuthTestUtils.registerAndLogin(client, AuthTestUtils.uniqueUsername(), "1234");
        aaplId = InstrumentTestUtils.resolveInstrumentId(client, token, "AAPL");
    }

    @Test
    void create_update_and_fetch_thesis() {
        // CREATE
        ThesisResponse created = client.post()
                .uri("/thesis")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of(
                        "instrumentId", aaplId,
                        "content", "Long term bullish due to ecosystem lock-in"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ThesisResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(created);
        assertEquals(aaplId, created.instrumentId());
        assertEquals("AAPL", created.ticker());
        assertEquals("Long term bullish due to ecosystem lock-in", created.content());

        // UPDATE (same endpoint = upsert)
        client.post()
                .uri("/thesis")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of(
                        "instrumentId", aaplId,
                        "content", "Revised thesis after earnings"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isEqualTo("Revised thesis after earnings");

        // FETCH
        client.get()
                .uri("/thesis/{instrumentId}", aaplId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ticker").isEqualTo("AAPL")
                .jsonPath("$.content").isEqualTo("Revised thesis after earnings");
    }

    @Test
    void fetch_thesis_that_doesnt_exist_returns_noContent() {
        client.get()
                .uri("/thesis/{instrumentId}", aaplId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void thesis_requires_auth() {
        client.post()
                .uri("/thesis")
                .bodyValue(Map.of("instrumentId", aaplId, "content", "test"))
                .exchange()
                .expectStatus().isUnauthorized();

        client.get()
                .uri("/thesis/{instrumentId}", aaplId)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}