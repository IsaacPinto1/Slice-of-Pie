package com.isaac.sliceofpie.instrument;

import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentResponse;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test helper for getting a real, resolved Instrument via the actual
 * /instruments/resolve endpoint
 *
 * Callers (ThesisFlowTest, future PositionFlowTest, etc.) should treat
 * "having an instrumentId" as a given precondition and never call
 * /instruments/resolve themselves 
 *
 * Requires InstrumentLookupClient to be mocked in the calling test class
 * (via @MockitoBean) with a stub for whichever ticker is passed in, since
 * this goes through the real resolution/persistence path, but shouldn't call
 * a real external API
 */
public final class InstrumentTestUtils {

    private InstrumentTestUtils() {}

    public static Long resolveInstrumentId(WebTestClient client, String token, String ticker) {
        InstrumentResponse response = client.post()
                .uri("/instruments/resolve")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("query", ticker))
                .exchange()
                .expectStatus().isOk()
                .expectBody(InstrumentResponse.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        return response.id();
    }
}