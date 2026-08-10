package com.isaac.sliceofpie.prices.finnhub;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class FinnhubDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubPriceResult(
            BigDecimal c,
            double t
    ) {}
    
}
