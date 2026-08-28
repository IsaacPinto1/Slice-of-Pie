package com.isaac.sliceofpie.prices;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.isaac.sliceofpie.instrument.InstrumentRepository;

/**
 * Updates prices in the background alongside the forced refreshes
 */
@Component
public class PriceRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshScheduler.class);

    public static final int REFRESH_WINDOW_MINUTES = PriceService.STALE_AFTER_MINUTES;

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

        // Instantly grab any instruments with null updated time rather than wait
        dueInstrumentIds.addAll(instrumentRepository.findIdsWithNullPriceUpdatedAt());

        for (Long instrumentId : dueInstrumentIds) {
            try {
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
