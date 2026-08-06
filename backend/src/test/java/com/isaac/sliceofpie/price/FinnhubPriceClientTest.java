package com.isaac.sliceofpie.price;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import org.hamcrest.Matchers;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import com.isaac.sliceofpie.prices.exception.TickerNotFoundException;
import com.isaac.sliceofpie.prices.finnhub.FinnhubPriceLookupClient;
import org.assertj.core.api.Assertions;

public class FinnhubPriceClientTest {

   private MockRestServiceServer mockServer; 
   private FinnhubPriceLookupClient client; 

    @BeforeEach 
    void setUp() { 
        RestClient.Builder builder = RestClient.builder(); 
        mockServer = MockRestServiceServer.bindTo(builder).build(); 
        client = new FinnhubPriceLookupClient("test-api-key", builder); 
    } 

    @Test 
    void throwsTickerNotFoundException_whenProviderCallFails() { 
        mockServer.expect(MockRestRequestMatchers.requestTo(Matchers.containsString("/quote")))
        .andRespond(MockRestResponseCreators.withStatus(HttpStatus.TOO_MANY_REQUESTS)); // simulates a Finnhub 429 

        Assertions.assertThatThrownBy(() -> client.getPrice("PLTR"))
                .isInstanceOf(TickerNotFoundException.class)
                .hasMessageContaining("PLTR");
    }

    @Test 
    void throwsTickerNotFoundException_whenTickerIsntFound() { 
        // Finnhub returns a success result with 0s and nulls if a ticker isn't found
        mockServer.expect(MockRestRequestMatchers.requestTo(Matchers.containsString("/quote")))
        .andRespond(MockRestResponseCreators.withSuccess("""
                {"c":0,"d":null,"dp":null,"h":0,"l":0,"o":0,"pc":0,"t":0}
                """, MediaType.APPLICATION_JSON)); 

        Assertions.assertThatThrownBy(() -> client.getPrice("zzz_notfound"))
                .isInstanceOf(TickerNotFoundException.class)
                .hasMessageContaining("zzz_notfound");
    }
}
