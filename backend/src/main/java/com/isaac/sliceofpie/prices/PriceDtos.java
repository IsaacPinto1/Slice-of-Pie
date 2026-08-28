package com.isaac.sliceofpie.prices;

import java.math.BigDecimal;
import java.time.Instant;

public class PriceDtos {

    // What a PriceLookupClient produces: just the number it read from the
    // provider (see FinnhubPriceLookupClient). A lookup is a single stateless
    // call out to a third party - it has no notion of when/whether that
    // number gets persisted, so there's no priceUpdatedAt here.
    public record PriceValueResponse(BigDecimal price) {
        public static PriceValueResponse from(int number){
            return new PriceValueResponse(new BigDecimal(number));
        }
        public static PriceValueResponse from(double number){
            return new PriceValueResponse(new BigDecimal(number));
        }
    }

    // What PriceService returns once a price is persisted onto an
    // Instrument (see PriceService.toResponse()). Every instance of this
    // goes through that one path, so priceUpdatedAt is never null here -
    // unlike PriceValueResponse, there's no raw/unpersisted variant to be
    // backward-compatible with.
    public record PricePersistedResponse(BigDecimal price, Instant priceUpdatedAt) {}
}
