package com.isaac.sliceofpie.prices;

import java.math.BigDecimal;
import java.time.Instant;

public class PriceDtos {

    // Just the number, returned by PriceLookupClient
    public record PriceValueResponse(BigDecimal price) {
        public static PriceValueResponse from(int number){
            return new PriceValueResponse(new BigDecimal(number));
        }
        public static PriceValueResponse from(double number){
            return new PriceValueResponse(new BigDecimal(number));
        }
    }

    // Price + updated time, produced by PriceService and used on frontend
    public record PricePersistedResponse(BigDecimal price, Instant priceUpdatedAt) {}
}
