package com.isaac.sliceofpie.price;

import com.isaac.sliceofpie.auth.AuthTestUtils;
import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentRepository;
import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Exercises /price through the real app - real Spring context, real
 * security filter chain, real routing - the way auth/thesis/watchlist are
 * already covered. Only proves what a full-stack test uniquely can:
 * PriceController is actually wired up behind /price and behind the JWT
 * filter, with PriceExceptionHandler picked up via component scanning
 * rather than added by hand.
 *
 * The exception -> ProblemDetail mapping itself (InvalidPriceException,
 * TickerNotFoundException, and the exact detail message each produces) is
 * covered more cheaply by PriceControllerTest, which wires the real
 * controller/service/handler together directly without booting the whole
 * app - no need to duplicate that here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PriceFlowTest {

    private WebTestClient client;

    // Stands in for the real FinnhubPriceLookupClient so /price can be
    // exercised end to end without making a real Finnhub call.
    @MockitoBean
    private PriceLookupClient priceLookupClient;

    @Autowired
    private InstrumentRepository instrumentRepository;

    private String token;

    // /price now takes an instrumentId, not a raw ticker - so unlike before,
    // getting a price requires a real, already-persisted Instrument row to
    // resolve the id from. Created directly via the repository rather than
    // through /instruments so this test isn't also exercising (and coupled
    // to) the search/create flow, which is covered separately.
    private Instrument instrument;
    private String ticker;

    @BeforeEach
    void setup(@LocalServerPort int port) {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        token = AuthTestUtils.registerAndLogin(client, AuthTestUtils.uniqueUsername(), "1234");
        ticker = "TST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        instrument = instrumentRepository.save(new Instrument(ticker, "Test Co", null));
    }

    @Test
    void getPrice_returnsPrice_whenLookupSucceeds() {
        when(priceLookupClient.getPrice(ticker))
                .thenReturn(new PriceResponse(new BigDecimal("158")));

        PriceResponse body = client.get()
                .uri("/price?instrumentId=" + instrument.getId())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PriceResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(body);
        assertEquals(0, new BigDecimal("158").compareTo(body.price()));
    }

    @Test
    void getPrice_requiresAuthentication() {
        client.get()
                .uri("/price?instrumentId=" + instrument.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
