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

    // How long a persisted price is considered fresh before getPrice() will
    // re-fetch it, and (via PriceRefreshScheduler.REFRESH_WINDOW_MINUTES)
    // how often the background job sweeps every instrument regardless of
    // whether anyone's actively looking at it. Public so both call sites -
    // and the frontend's staleness check, via staleAfterMinutes on
    // PriceResponse - stay in lockstep with a single number instead of
    // drifting apart.
    public static final int STALE_AFTER_MINUTES = 60;

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
    * Keeping this untransactional means a failure here doesn't
    * derail instrument creation (which happens even if an exception
    * in a @Transactional is caught)
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
    * Returns cached price with above defined timeout, or looks up latest price otherwise
    */
    @Transactional
    public PriceResponse getPrice(Long instrumentId){
        Instrument instrument = instrumentResolutionService.getById(instrumentId);

        if (instrument.getPriceUpdatedAt() != null &&
            Duration.between(instrument.getPriceUpdatedAt(), Instant.now()).compareTo(Duration.ofMinutes(STALE_AFTER_MINUTES)) < 0) {

            return toResponse(instrument);
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
        // Build the response from the instrument itself, not the raw
        // provider response - res has no priceUpdatedAt (see
        // PriceResponse's 1-arg constructor), while instrument.setPrice()
        // just stamped a real one.
        return toResponse(instrument);
    }

    private static PriceResponse toResponse(Instrument instrument) {
        return new PriceResponse(
                BigDecimal.valueOf(instrument.getPrice()),
                instrument.getPriceUpdatedAt(),
                STALE_AFTER_MINUTES
        );
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
