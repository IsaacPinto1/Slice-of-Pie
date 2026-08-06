package com.isaac.sliceofpie.prices.finnhub;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.isaac.sliceofpie.instrument.finnhub.FinnhubDtos.FinnhubPriceResult;
import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.exception.TickerNotFoundException;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;

@Component
public class FinnhubPriceLookupClient implements PriceLookupClient{
    
    private static final String BASE_URL = "https://finnhub.io/api/v1";

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubPriceLookupClient(@Value("${finnhub.api.key}") String apiKey,
                                    RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .build();
    }

    @Override
    public PriceResponse getPrice(String ticker){
        FinnhubPriceResult response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", ticker)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubPriceResult.class);
        } catch (RestClientException e) {
            // Covers 4xx/5xx (including Finnhub rate-limit 429s) and network failures.
            throw new TickerNotFoundException("Price not found for ticker '" + ticker + "'", e);
        }

        if (response == null || response.t() == 0) {
            throw new TickerNotFoundException(
                    "Price not found for ticker '" + ticker + "'");
        }

        // Mapping happens right here, at the edge - nothing downstream ever
        // sees a raw Finnhub shape.
        return new PriceResponse(response.c());
    }
}
