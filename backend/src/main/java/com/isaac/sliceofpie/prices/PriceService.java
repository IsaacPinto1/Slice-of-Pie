package com.isaac.sliceofpie.prices;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;
import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.exception.InvalidPriceException;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;


@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);

    private final PriceLookupClient priceLookupClient;

    private final InstrumentResolutionService instrumentResolutionService;

    private final int PRICE_LIFETIME_HOURS = 1;

    public PriceService(PriceLookupClient priceLookupClient, InstrumentResolutionService instrumentResolutionService){
        this.priceLookupClient = priceLookupClient;
        this. instrumentResolutionService= instrumentResolutionService;
    }

    /*
    * Best-effort price fetch for use at instrument-creation time (see
    * InstrumentResolutionService.createInstrument()). Deliberately NOT
    * @Transactional and never throws: it's called from inside that
    * method's own transaction, on an already-managed Instrument, so the
    * caller can just apply the result directly (instrument.setPrice(...))
    * without a second transaction/persistence-context involved.
    *
    * Routing this through a @Transactional method instead (e.g.
    * forceLatestPrice) would be risky here even wrapped in try/catch:
    * Spring's transactional AOP marks a participating transaction
    * rollback-only the moment an exception leaves a @Transactional method,
    * REGARDLESS of whether the caller then catches it - so instrument
    * creation could silently fail to commit even though the exception
    * looked handled. Keeping this fetch untransactional avoids that trap.
    *
    * Returns empty on any failure (invalid price, provider error, etc.) -
    * the caller leaves the instrument at price=0/priceUpdatedAt=null in
    * that case, and PriceRefreshScheduler's null-price sweep retries it on
    * its next tick.
    */
    public Optional<BigDecimal> tryFetchPrice(String ticker) {
        try {
            PriceResponse res = priceLookupClient.getPrice(ticker);
            if (!isValidPrice(res.price())) {
                return Optional.empty();
            }
            return Optional.of(res.price());
        } catch (Exception e) {
            log.warn("Failed to fetch initial price for ticker '{}'", ticker, e);
            return Optional.empty();
        }
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

        if (!isValidPrice(res.price())) {
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

    private static boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) >= 0;
    }
}
