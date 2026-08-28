package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.instrument.Instrument;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class WatchlistDtos {

    public record WatchlistItemResponse(
            Long instrumentId,
            String ticker, // Ok to store this here, since it is not stored but retrieved from the instrument
            String name,
            BigDecimal price, // Dumb db lookup so can render instantly
            Instant priceUpdatedAt, // Lets frontend show how recent the price is
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
                    item.getCreatedAt()
            );
        }
    }

    public record WatchlistResponse(
            List<WatchlistItemResponse> items
    ) {}
}