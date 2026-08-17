package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.instrument.Instrument;

import java.math.BigDecimal;
import java.util.List;

// Single DTO file for the whole broker/positions feature - status,
// reading positions, and the provider-agnostic shape a BrokerClient
// hands back internally. See PositionController / PositionService.
public class PositionDtos {

    public record BrokerStatusResponse(boolean connected) {}

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
            BigDecimal costbasis
    ) {
        public static PositionItemResponse from(Position position) {
            Instrument instrument = position.getInstrument();
            return new PositionItemResponse(
                    instrument.getId(),
                    instrument.getTicker(),
                    instrument.getName(),
                    position.getQuantity(),
                    BigDecimal.valueOf(instrument.getPrice()),
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
