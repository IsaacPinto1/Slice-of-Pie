package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.prices.PriceService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Single DTO file for the whole broker/positions feature - status,
// reading positions, and the provider-agnostic shape a BrokerClient
// hands back internally. See PositionController / PositionService.
public class PositionDtos {

    public record BrokerStatusResponse(boolean connected) {}

    // Cheap, non-throwing counterpart to BrokerAccessGuard#assertAllowed -
    // just an env-var Set#contains check, no external call. Meant to be
    // fetched alongside /me and /watchlist on initial load so the frontend
    // knows whether to render the Positions section at all before it ever
    // touches /broker/status (which can be slow/flaky - it calls out to
    // the actual provider once connected). Deliberately a plain 200 with a
    // boolean rather than the 404-if-not-allowed treatment the other
    // broker routes use - see BrokerAccessGuard's javadoc for why those
    // hide behind a 404, and PositionController for why this one doesn't.
    public record BrokerAllowedResponse(boolean allowed) {}

    public record PositionItemResponse(
            Long instrumentId,
            String ticker,
            String name,
            BigDecimal quantity,
            // Read straight off the Instrument row instead of the live
            // provider - see WatchlistDtos.WatchlistItemResponse.price for
            // the full reasoning (kept once there since both DTOs pull it
            // the same way).
            BigDecimal price,
            // Mirrors WatchlistDtos.WatchlistItemResponse - see there for why.
            Instant priceUpdatedAt,
            int staleAfterMinutes,
            BigDecimal costBasis
    ) {
        public static PositionItemResponse from(Position position) {
            Instrument instrument = position.getInstrument();
            return new PositionItemResponse(
                    instrument.getId(),
                    instrument.getTicker(),
                    instrument.getName(),
                    position.getQuantity(),
                    BigDecimal.valueOf(instrument.getPrice()),
                    instrument.getPriceUpdatedAt(),
                    PriceService.STALE_AFTER_MINUTES,
                    position.getCostBasis()
            );
        }
    }

    public record PositionResponse(List<PositionItemResponse> items) {}

    // Normalized shape any BrokerClient implementation maps its provider's
    // holdings/positions response into, before PositionService ever sees a
    // provider-specific shape. Same "mapping happens at the edge" pattern
    // as InstrumentDtos.InstrumentSearchResult / PriceDtos.PriceResponse.
    public record BrokerHolding(String ticker, String name, BigDecimal quantity, BigDecimal costBasis) {}
}
