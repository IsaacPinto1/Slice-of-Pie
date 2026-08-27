package com.isaac.sliceofpie.prices;

import java.math.BigDecimal;
import java.time.Instant;

public class PriceDtos {

    public record PriceResponse(BigDecimal price, Instant priceUpdatedAt, int staleAfterMinutes){
        // Backward-compatible constructor for the raw provider-lookup path
        // (FinnhubPriceLookupClient etc, which has no notion of a persisted
        // priceUpdatedAt) and existing tests that only care about price.
        public PriceResponse(BigDecimal price) {
            this(price, null, PriceService.STALE_AFTER_MINUTES);
        }

        public static PriceResponse from(int number){
            BigDecimal bigPrice = new BigDecimal(number);
            return new PriceResponse(bigPrice);
        }
        public static PriceResponse from(double number){
            BigDecimal bigPrice = new BigDecimal(number);
            return new PriceResponse(bigPrice);
        }
    }
}
