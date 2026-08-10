package com.isaac.sliceofpie.prices.lookup;

import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;

public interface PriceLookupClient {
    PriceResponse getPrice(String ticker);
}
