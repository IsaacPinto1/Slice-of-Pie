package com.isaac.sliceofpie.instrument.finnhub;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FinnhubDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FinnhubSearchResponse(
            int count,
            List<FinnhubSearchResult> result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FinnhubSearchResult(
            String description,
            @JsonProperty("displaySymbol") String displaySymbol,
            String symbol,
            String type
    ) {}
    
}
