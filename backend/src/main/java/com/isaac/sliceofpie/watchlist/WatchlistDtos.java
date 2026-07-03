package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.instrument.Instrument;

import java.time.Instant;
import java.util.List;

public class WatchlistDtos {

    public record WatchlistItemResponse(
            Long instrumentId,
            String ticker,
            String name,
            Instant createdAt
    ) {
        public static WatchlistItemResponse from(WatchlistItem item) {
            Instrument instrument = item.getInstrument();
            return new WatchlistItemResponse(
                    instrument.getId(),
                    instrument.getTicker(),
                    instrument.getName(),
                    item.getCreatedAt()
            );
        }
    }

    public record WatchlistResponse(
            List<Long> instrumentIds
    ) {}
}