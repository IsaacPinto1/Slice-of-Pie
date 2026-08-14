package com.isaac.sliceofpie.prices;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.isaac.sliceofpie.instrument.InstrumentRepository;

/**
 * Keeps stored prices from going stale even for instruments nobody is
 * actively looking at, without hammering the price provider all at once.
 *
 * Every instrument needs a refresh roughly every REFRESH_WINDOW_MINUTES
 * (3 hours = 180 minutes). Rather than refreshing all of them in the same
 * tick - which would burst-call the provider for every instrument in the
 * system simultaneously - each instrument is assigned a fixed minute-slot
 * within that window via (id % REFRESH_WINDOW_MINUTES). This job runs once
 * a minute and only refreshes whichever slot the current minute falls
 * into, spreading one 180-minute cycle's worth of provider calls evenly
 * across the whole window instead of bursting them every 3 hours.
 *
 * This is independent of (and in addition to) the on-demand caching in
 * PriceService.getPrice: that keeps a price fresh for someone actively
 * looking at it right now; this keeps every instrument from drifting too
 * far out of date even when nobody is.
 */
@Component
public class PriceRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshScheduler.class);

    // 3 hours, expressed in minutes, since the job ticks once a minute.
    // Public (not package-private, unlike e.g. InstrumentResolutionService's
    // MAX_SEARCH_RESULTS) because PriceRefreshSchedulerTest lives in
    // com.isaac.sliceofpie.price, not this class's com.isaac.sliceofpie.prices
    // package - the existing split for this feature's tests.
    public static final int REFRESH_WINDOW_MINUTES = 180;

    private final InstrumentRepository instrumentRepository;
    private final PriceService priceService;

    public PriceRefreshScheduler(InstrumentRepository instrumentRepository, PriceService priceService) {
        this.instrumentRepository = instrumentRepository;
        this.priceService = priceService;
    }

    // Fires at the top of every minute.
    @Scheduled(cron = "0 * * * * *")
    public void refreshDueInstruments() {
        int slot = currentSlot();
        Set<Long> dueInstrumentIds = new LinkedHashSet<>(
                instrumentRepository.findIdsByIdModulo(REFRESH_WINDOW_MINUTES, slot));

        // A brand-new instrument (create() leaves price=0, priceUpdatedAt
        // null) only lands in the slot rotation above once its id's
        // assigned minute comes around - up to REFRESH_WINDOW_MINUTES (3h)
        // later. Fold in every never-fetched instrument on every tick so it
        // gets a real price within a minute of being created instead.
        dueInstrumentIds.addAll(instrumentRepository.findIdsWithNullPriceUpdatedAt());

        for (Long instrumentId : dueInstrumentIds) {
            try {
                // Goes through the same public, transactional entry point
                // PriceController's /price/force uses - re-fetches the
                // instrument itself so it stays attached to the
                // transaction that persists the new price.
                priceService.forceLatestPrice(instrumentId);
            } catch (Exception e) {
                // One bad ticker or provider hiccup shouldn't stop the rest
                // of this minute's batch from refreshing.
                log.warn("Scheduled price refresh failed for instrument id={}: {}", instrumentId, e.getMessage());
            }
        }
    }

    private static int currentSlot() {
        return (int) ((Instant.now().getEpochSecond() / 60) % REFRESH_WINDOW_MINUTES);
    }
}
