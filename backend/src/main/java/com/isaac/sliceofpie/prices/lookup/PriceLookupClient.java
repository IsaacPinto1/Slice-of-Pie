package com.isaac.sliceofpie.prices.lookup;

import com.isaac.sliceofpie.prices.PriceDtos.PriceValueResponse;

public interface PriceLookupClient {
    PriceValueResponse getPrice(String ticker);
}
