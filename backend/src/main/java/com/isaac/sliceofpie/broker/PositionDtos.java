package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.instrument.Instrument;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Single DTO file for the whole broker/positions feature - status,
// reading positions, and the provider-agnostic shape a BrokerClient
// hands back internally. See PositionController / PositionService.
public class PositionDtos {

    public record BrokerStatusResponse(boolean connected) {}

    // Simple check if the user is allowed to use positions feature
    public record BrokerAllowedResponse(boolean allowed) {}

    public record PositionItemResponse(
            Long instrumentId,
            String ticker,
            String name,
            BigDecimal quantity,
            BigDecimal price, // Dumb db lookup
            Instant priceUpdatedAt,
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
