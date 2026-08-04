package com.isaac.sliceofpie.instrument;

import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentResponse;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test helper for getting a real, created Instrument via the actual
 * POST /instruments endpoint (the only path that creates an Instrument now
 * - search -> select -> create).
 *
 * Callers (ThesisFlowTest, WatchlistFlowTest, future PositionFlowTest, etc.)
 * should treat "having an instrumentId" as a given precondition and never
 * call POST /instruments themselves.
 *
 * Since create() no longer calls the lookup provider (ticker + name are
 * supplied directly, as if they came from a selected search result), this
 * doesn't require InstrumentLookupClient to be stubbed - only mocked/present
 * as a bean so the real FinnhubInstrumentLookupClient (which needs a real
 * API key) isn't instantiated in the test context.
 */
public final class InstrumentTestUtils {

    private InstrumentTestUtils() {}

    public static Long resolveInstrumentId(WebTestClient client, String token, String ticker) {
        return resolveInstrumentId(client, token, ticker, ticker + " TEST CO");
    }

    public static Long resolveInstrumentId(WebTestClient client, String token, String ticker, String name) {
        InstrumentResponse response = client.post()
                .uri("/instruments")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("ticker", ticker, "name", name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(InstrumentResponse.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        return response.id();
    }
}