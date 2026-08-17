package com.isaac.sliceofpie.broker;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.isaac.sliceofpie.broker.PositionDtos.BrokerHolding;
import com.isaac.sliceofpie.broker.exception.BrokerLookupException;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeAccountClient;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeSigningClient;
import com.isaac.sliceofpie.config.SnapTradeClientConfig;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mirrors FinnhubPriceClientTest/FinnhubInstrumentClientTest's shape:
 * exercises the real SnapTradeAccountClient (and the real
 * SnapTradeSigningClient underneath it) against a MockRestServiceServer
 * instead of the real SnapTrade API, so SnapTrade's actual response shapes
 * (bare account arrays, {"results": [...]} position envelopes, cash-like
 * positions with a null instrument/symbol/units) are exercised without a
 * live network call or real Personal key.
 *
 * PositionControllerTest/PositionFlowTest cover the rest of the stack with
 * BrokerClient mocked out entirely - this is the one place SnapTrade's
 * actual wire format is asserted against.
 */
class SnapTradeAccountClientTest {

    private MockRestServiceServer mockServer;
    private SnapTradeAccountClient client;

    @BeforeEach
    void setUp() {
        // Mirrors what SnapTradeClientConfig.snapTradeRestClientBuilder does
        // in production - the signing client itself no longer sets a base
        // URL.
        RestClient.Builder builder = RestClient.builder().baseUrl(SnapTradeClientConfig.SNAPTRADE_BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();

        ObjectMapper objectMapper = JsonMapper.builder().build();
        SnapTradeSigningClient signingClient =
                new SnapTradeSigningClient(builder, objectMapper, "test-client-id", "test-consumer-key");
        client = new SnapTradeAccountClient(signingClient);
    }

    @Test
    void hasConnectedAccounts_returnsTrue_whenProviderReportsAccounts() {
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("""
                        [{"id":"acct-1","name":"Robinhood"}]
                        """, MediaType.APPLICATION_JSON));

        assertTrue(client.hasConnectedAccounts());
    }

    @Test
    void hasConnectedAccounts_returnsFalse_whenProviderReportsNoAccounts() {
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertFalse(client.hasConnectedAccounts());
    }

    @Test
    void fetchHoldings_returnsEmptyList_whenNoAccountsConnected() {
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.fetchHoldings().isEmpty());
    }

    @Test
    void fetchHoldings_sumsSameTickerAcrossMultipleAccounts() {
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("""
                        [{"id":"acct-1","name":"Robinhood"},{"id":"acct-2","name":"Robinhood IRA"}]
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(containsString("/accounts/acct-1/positions/all")))
                .andRespond(withSuccess("""
                        {"results":[{"instrument":{"symbol":"AAPL","description":"Apple Inc"},"units":5,"cost_basis":100}]}
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(containsString("/accounts/acct-2/positions/all")))
                .andRespond(withSuccess("""
                        {"results":[{"instrument":{"symbol":"AAPL","description":"Apple Inc"},"units":3.5,"cost_basis":120}]}
                        """, MediaType.APPLICATION_JSON));

        List<BrokerHolding> holdings = client.fetchHoldings();

        assertEquals(1, holdings.size());
        assertEquals("AAPL", holdings.get(0).ticker());
        assertEquals(0, new BigDecimal("8.5").compareTo(holdings.get(0).quantity()));
        // Weighted average across the two accounts: (5*100 + 3.5*120) / 8.5
        assertEquals(0, new BigDecimal("108.23529412").compareTo(holdings.get(0).costBasis()));
    }

    @Test
    void fetchHoldings_defaultsCostBasisToZero_whenProviderOmitsIt() {
        // Some SnapTrade position shapes don't include cost_basis at all -
        // it must default to zero rather than blow up the sync (Position's
        // cost_basis column is NOT NULL).
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("""
                        [{"id":"acct-1","name":"Robinhood"}]
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(containsString("/accounts/acct-1/positions/all")))
                .andRespond(withSuccess("""
                        {"results":[{"instrument":{"symbol":"AAPL","description":"Apple Inc"},"units":5}]}
                        """, MediaType.APPLICATION_JSON));

        List<BrokerHolding> holdings = client.fetchHoldings();

        assertEquals(1, holdings.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(holdings.get(0).costBasis()));
    }

    @Test
    void fetchHoldings_readsCostBasis_fromSnapTradePositionShape() {
        // Mirrors the documented snaptrade positions/all response shape:
        // {"units": "0.472165", "price": "225.17", "cost_basis": "211.79", ...}
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("""
                        [{"id":"acct-1","name":"Robinhood"}]
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(containsString("/accounts/acct-1/positions/all")))
                .andRespond(withSuccess("""
                        {"results":[{"instrument":{"symbol":"AAPL","description":"Apple Inc"},"units":"0.472165","price":"225.17","cost_basis":"211.79","currency":"USD"}]}
                        """, MediaType.APPLICATION_JSON));

        List<BrokerHolding> holdings = client.fetchHoldings();

        assertEquals(1, holdings.size());
        assertEquals(0, new BigDecimal("211.79").compareTo(holdings.get(0).costBasis()));
    }

    @Test
    void fetchHoldings_skipsCashAndOtherNonInstrumentPositions() {
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("""
                        [{"id":"acct-1","name":"Robinhood"}]
                        """, MediaType.APPLICATION_JSON));

        // A null instrument, a null symbol (cash sweep), and null units
        // should all be skipped rather than blow up the whole sync - only
        // the last MSFT entry should survive.
        mockServer.expect(requestTo(containsString("/accounts/acct-1/positions/all")))
                .andRespond(withSuccess("""
                        {"results":[
                            {"instrument":null,"units":10},
                            {"instrument":{"symbol":null,"description":"Cash Sweep"},"units":100},
                            {"instrument":{"symbol":"MSFT","description":"Microsoft"},"units":null},
                            {"instrument":{"symbol":"MSFT","description":"Microsoft"},"units":2}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<BrokerHolding> holdings = client.fetchHoldings();

        assertEquals(1, holdings.size());
        assertEquals("MSFT", holdings.get(0).ticker());
        assertEquals(0, new BigDecimal("2").compareTo(holdings.get(0).quantity()));
    }

    @Test
    void hasConnectedAccounts_throwsBrokerLookupException_whenAccountsCallFails() {
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)); // simulates a SnapTrade 5xx

        Assertions.assertThatThrownBy(() -> client.hasConnectedAccounts())
                .isInstanceOf(BrokerLookupException.class);
    }

    @Test
    void fetchHoldings_throwsBrokerLookupException_whenPositionsCallFails() {
        mockServer.expect(requestTo(containsString("/accounts")))
                .andRespond(withSuccess("""
                        [{"id":"acct-1","name":"Robinhood"}]
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(containsString("/accounts/acct-1/positions/all")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)); // simulates a SnapTrade 429

        Assertions.assertThatThrownBy(() -> client.fetchHoldings())
                .isInstanceOf(BrokerLookupException.class);
    }
}
