package com.isaac.sliceofpie.prices;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.exception.InvalidPriceException;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;

@Service
public class PriceService {

    private final PriceLookupClient priceLookupClient;

    public PriceService(PriceLookupClient priceLookupClient){
        this.priceLookupClient = priceLookupClient;
    }

    public PriceResponse getPrice(String ticker){
        PriceResponse res = priceLookupClient.getPrice(ticker);

        if(res.price() == null || res.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPriceException("Retrieved price invalid for ticker '" + ticker + "'");
        }

        return res;
    }

    
}
