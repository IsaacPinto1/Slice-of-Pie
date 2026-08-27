package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.prices.PriceService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class WatchlistDtos {

    public record WatchlistItemResponse(
            Long instrumentId,
            String ticker, // Ok to store this here, since it is not stored but retrieved from the instrument
            String name,
            // Last price PriceService persisted onto the Instrument row, not
            // a live provider lookup - same "read what's already in the db"
            // shape as the rest of this DTO. Lets a list of items render
            // ticker + price straight from GET /watchlist (or /positions),
            // instead of the client following up with a GET /price call per
            // item on every load/refresh. Still just a snapshot: a client
            // that wants the guaranteed-latest number for one focused item
            // can hit /price or /price/force for that item specifically.
            BigDecimal price,
            // priceUpdatedAt/staleAfterMinutes mirror PriceDtos.PriceResponse
            // so the frontend can flag a stale price wherever it's shown,
            // not just right after a manual /price/force refresh - this is
            // the default source for the sidebar/detail panels.
            Instant priceUpdatedAt,
            int staleAfterMinutes,
            Instant createdAt
    ) {
        public static WatchlistItemResponse from(WatchlistItem item) {
            Instrument instrument = item.getInstrument();
            return new WatchlistItemResponse(
                    instrument.getId(),
                    instrument.getTicker(),
                    instrument.getName(),
                    BigDecimal.valueOf(instrument.getPrice()),
                    instrument.getPriceUpdatedAt(),
                    PriceService.STALE_AFTER_MINUTES,
                    item.getCreatedAt()
            );
        }
    }

    public record WatchlistResponse(
            List<WatchlistItemResponse> items
    ) {}
}