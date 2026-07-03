package com.isaac.sliceofpie.instrument.finnhub;

import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;
import com.isaac.sliceofpie.instrument.exception.InstrumentLookupException;
import com.isaac.sliceofpie.instrument.finnhub.FinnhubDtos.FinnhubSearchResponse;
import com.isaac.sliceofpie.instrument.lookup.InstrumentLookupClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Finnhub-backed implementation of InstrumentLookupClient.
 *
 * This package is the only place in the codebase that should know Finnhub's
 * API shape. Everything outside it talks to InstrumentLookupClient /
 * InstrumentSearchResult - swapping providers means writing a new
 * implementation of that interface and rewiring the bean, nothing else
 * should need to change.
 *
 * Requires FINNHUB_API_KEY to be set as an environment variable and wired
 * through application.yml, e.g.:
 *
 *   finnhub:
 *     api:
 *       key: ${FINNHUB_API_KEY}
 *
 * Note: uses Spring's RestClient (Spring Framework 6.1+ / Boot 3.2+).
 * If the project is on an older Boot version, swap this for RestTemplate.
 */
@Component
public class FinnhubInstrumentLookupClient implements InstrumentLookupClient {

    private static final String BASE_URL = "https://finnhub.io/api/v1";

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubInstrumentLookupClient(@Value("${finnhub.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    @Override
    public List<InstrumentSearchResult> search(String query) {
        FinnhubSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubSearchResponse.class);
        } catch (RestClientException e) {
            // Covers 4xx/5xx (including Finnhub rate-limit 429s) and network failures.
            throw new InstrumentLookupException("Instrument search failed for query '" + query + "'", e);
        }

        if (response == null || response.result() == null) {
            return List.of();
        }

        // Mapping happens right here, at the edge - nothing downstream ever
        // sees a raw Finnhub shape.
        return response.result().stream()
                .map(r -> new InstrumentSearchResult(r.symbol(), r.description()))
                .toList();
    }
}