package com.isaac.sliceofpie.prices;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;
import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.exception.InvalidPriceException;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;


@Service
public class PriceService {

    private final PriceLookupClient priceLookupClient;

    private final InstrumentResolutionService instrumentResolutionService;

    private final int PRICE_LIFETIME_HOURS = 1;

    public PriceService(PriceLookupClient priceLookupClient, InstrumentResolutionService instrumentResolutionService){
        this.priceLookupClient = priceLookupClient;
        this. instrumentResolutionService= instrumentResolutionService;
    }

    /*
    * Returns cached price with above defined hour timeout, or looks up latest price otherwise
    */
    @Transactional
    public PriceResponse getPrice(Long instrumentId){
        Instrument instrument = instrumentResolutionService.getById(instrumentId);

        if (instrument.getPriceUpdatedAt() != null &&
            Duration.between(instrument.getPriceUpdatedAt(), Instant.now()).compareTo(Duration.ofHours(PRICE_LIFETIME_HOURS )) < 0) {

            return PriceResponse.from(instrument.getPrice());
        }

        return forceLatestPrice(instrument);
    }

    @Transactional
    public PriceResponse forceLatestPrice(Instrument instrument) {
        String ticker = instrument.getTicker();
        PriceResponse res = priceLookupClient.getPrice(ticker);

        if(res.price() == null || res.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPriceException("Retrieved price invalid for ticker '" + ticker + "'");
        }

        instrument.setPrice(res.price().doubleValue());
        return res;
    }

    /*
    * Allow for forced updates
     */
    @Transactional
    public PriceResponse forceLatestPrice(Long instrumentId) {
        Instrument instrument = instrumentResolutionService.getById(instrumentId);
        return forceLatestPrice(instrument);
    }
}
