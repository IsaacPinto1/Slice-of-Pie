package com.isaac.sliceofpie.instrument.finnhub;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class FinnhubDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FinnhubSearchResponse(
            int count,
            List<FinnhubSearchResult> result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FinnhubSearchResult(
            String description,
            String displaySymbol,
            String symbol,
            String type
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubPriceResult(
            BigDecimal c
    ) {}
    
}
