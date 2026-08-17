package com.isaac.sliceofpie.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.isaac.sliceofpie.auth.AuthTestUtils;
import com.isaac.sliceofpie.broker.PositionDtos.BrokerHolding;
import com.isaac.sliceofpie.broker.PositionDtos.BrokerStatusResponse;
import com.isaac.sliceofpie.broker.PositionDtos.PositionResponse;
import com.isaac.sliceofpie.broker.lookup.BrokerClient;

/**
 * Exercises /broker/status, /positions, and /positions/sync through the
 * real app - real Spring context, real security filter chain, real
 * routing, real BrokerAccessGuard allowlist - the way price/watchlist are
 * already covered. Only BrokerClient is mocked, standing in for the real
 * SnapTradeAccountClient so this can run without real SnapTrade
 * credentials or a real Personal key connection.
 *
 * The allowlist is a fixed username set once via @DynamicPropertySource
 * (BrokerAccessGuard's Set is built at context startup, so it can't vary
 * per test method) - ALLOWED_USERNAME is registered once in @BeforeAll and
 * its token reused across every "allowed user" test, while each
 * "not allowed" test registers its own throwaway user to prove the feature
 * really is invisible to everyone else.
 *
 * The exception -> ProblemDetail mapping itself (BrokerNotConnectedException,
 * BrokerLookupException, RequestSigningException, BrokerAccessDeniedException)
 * is covered more cheaply by PositionControllerTest, which wires the real
 * controller/service/handler together directly without booting the whole
 * app - no need to duplicate that here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PositionFlowTest {

    private static final String ALLOWED_USERNAME = "broker_flow_test_user";

    @DynamicPropertySource
    static void allowlist(DynamicPropertyRegistry registry) {
        registry.add("snaptrade.allowed-usernames", () -> ALLOWED_USERNAME);
    }

    private WebTestClient client;

    // Stands in for the real SnapTradeAccountClient so the broker routes
    // can be exercised end to end without a real SnapTrade Personal key.
    @MockitoBean
    private BrokerClient brokerClient;

    private String allowedToken;

    @BeforeAll
    void registerAllowedUser(@LocalServerPort int port) {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        allowedToken = AuthTestUtils.registerAndLogin(client, ALLOWED_USERNAME, "1234");
    }

    @BeforeEach
    void setup(@LocalServerPort int port) {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void broker_routes_requireAuthentication() {
        client.get().uri("/broker/status").exchange().expectStatus().isUnauthorized();
        client.get().uri("/positions").exchange().expectStatus().isUnauthorized();
        client.post().uri("/positions/sync").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void broker_routes_areInvisible_toNonAllowedUser() {
        // Authenticated, but not on the allowlist - every broker route
        // must look exactly like a route that doesn't exist (404, not
        // 403). See BrokerAccessGuard/BrokerAccessDeniedException.
        String token = AuthTestUtils.registerAndLogin(client, AuthTestUtils.uniqueUsername(), "1234");

        client.get().uri("/broker/status")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();

        client.get().uri("/positions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();

        client.post().uri("/positions/sync")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void status_reportsProviderConnectionState_forAllowedUser() {
        when(brokerClient.hasConnectedAccounts()).thenReturn(true);

        BrokerStatusResponse body = client.get()
                .uri("/broker/status")
                .header("Authorization", "Bearer " + allowedToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BrokerStatusResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(body);
        assertEquals(true, body.connected());
    }

    @Test
    void sync_returnsConflict_whenProviderHasNoConnections() {
        when(brokerClient.hasConnectedAccounts()).thenReturn(false);

        client.post()
                .uri("/positions/sync")
                .header("Authorization", "Bearer " + allowedToken)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void sync_pullsHoldings_andPersistsAsPositions() {
        when(brokerClient.hasConnectedAccounts()).thenReturn(true);
        when(brokerClient.fetchHoldings())
                .thenReturn(List.of(new BrokerHolding("AAPL", "Apple Inc", new BigDecimal("5"), new BigDecimal("150.25"))));

        PositionResponse synced = client.post()
                .uri("/positions/sync")
                .header("Authorization", "Bearer " + allowedToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PositionResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(synced);
        assertEquals(1, synced.items().size());
        assertEquals("AAPL", synced.items().get(0).ticker());
        assertEquals(0, new BigDecimal("5").compareTo(synced.items().get(0).quantity()));
        assertEquals(0, new BigDecimal("150.25").compareTo(synced.items().get(0).costBasis()));

        // GET /positions afterward reflects what sync persisted, without
        // calling the provider again.
        PositionResponse fetched = client.get()
                .uri("/positions")
                .header("Authorization", "Bearer " + allowedToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PositionResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(fetched);
        assertEquals(1, fetched.items().size());
        assertEquals("AAPL", fetched.items().get(0).ticker());
        assertEquals(0, new BigDecimal("150.25").compareTo(fetched.items().get(0).costBasis()));
    }

    @Test
    void sync_reconciles_removingPositionsNoLongerHeld() {
        when(brokerClient.hasConnectedAccounts()).thenReturn(true);
        when(brokerClient.fetchHoldings())
                .thenReturn(List.of(
                        new BrokerHolding("AAPL", "Apple Inc", new BigDecimal("5"), new BigDecimal("150.25")),
                        new BrokerHolding("TSLA", "Tesla Inc", new BigDecimal("2"), new BigDecimal("220.00"))));

        client.post()
                .uri("/positions/sync")
                .header("Authorization", "Bearer " + allowedToken)
                .exchange()
                .expectStatus().isOk();

        // Second sync only reports AAPL - TSLA must be reconciled away.
        when(brokerClient.fetchHoldings())
                .thenReturn(List.of(new BrokerHolding("AAPL", "Apple Inc", new BigDecimal("5"), new BigDecimal("150.25"))));

        PositionResponse resynced = client.post()
                .uri("/positions/sync")
                .header("Authorization", "Bearer " + allowedToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PositionResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(resynced);
        assertEquals(1, resynced.items().size());
        assertEquals("AAPL", resynced.items().get(0).ticker());
    }
}
