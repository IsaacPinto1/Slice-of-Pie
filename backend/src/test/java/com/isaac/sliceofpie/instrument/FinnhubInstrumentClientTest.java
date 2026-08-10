package com.isaac.sliceofpie.instrument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import org.hamcrest.Matchers;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import com.isaac.sliceofpie.config.FinnhubClientConfig;
import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;
import com.isaac.sliceofpie.instrument.exception.InstrumentLookupException;
import com.isaac.sliceofpie.instrument.finnhub.FinnhubInstrumentLookupClient;
import org.assertj.core.api.Assertions;

import java.util.List;

/**
 * Mirrors FinnhubPriceClientTest - now that FinnhubInstrumentLookupClient
 * takes an injected RestClient.Builder instead of calling
 * RestClient.builder() itself, it can be exercised the same way as
 * FinnhubPriceLookupClient, against a MockRestServiceServer instead of the
 * real Finnhub API.
 */
public class FinnhubInstrumentClientTest {

    private MockRestServiceServer mockServer;
    private FinnhubInstrumentLookupClient client;

    @BeforeEach
    void setUp() {
        // Mirrors what FinnhubClientConfig.finnhubRestClientBuilder does in
        // production - the client itself no longer sets a base URL.
        RestClient.Builder builder = RestClient.builder().baseUrl(FinnhubClientConfig.FINNHUB_BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new FinnhubInstrumentLookupClient("test-api-key", builder);
    }

    @Test
    void returnsMappedResults_whenProviderCallSucceeds() {
        mockServer.expect(MockRestRequestMatchers.requestTo(Matchers.containsString("/search")))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"count":1,"result":[{"description":"APPLE INC","displaySymbol":"AAPL","symbol":"AAPL","type":"Common Stock"}]}
                        """, MediaType.APPLICATION_JSON));

        List<InstrumentSearchResult> results = client.search("AAPL");

        Assertions.assertThat(results)
                .containsExactly(new InstrumentSearchResult("AAPL", "APPLE INC"));
    }

    @Test
    void throwsInstrumentLookupException_whenProviderCallFails() {
        mockServer.expect(MockRestRequestMatchers.requestTo(Matchers.containsString("/search")))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.TOO_MANY_REQUESTS)); // simulates a Finnhub 429

        Assertions.assertThatThrownBy(() -> client.search("AAPL"))
                .isInstanceOf(InstrumentLookupException.class)
                .hasMessageContaining("AAPL");
    }
}
