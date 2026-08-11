package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.instrument.Instrument;

import java.math.BigDecimal;
import java.util.List;

public class PositionDtos {

    public record PositionItemResponse(
            Long instrumentId,
            String ticker,
            String name,
            BigDecimal quantity
    ) {
        public static PositionItemResponse from(Position position) {
            Instrument instrument = position.getInstrument();
            return new PositionItemResponse(
                    instrument.getId(),
                    instrument.getTicker(),
                    instrument.getName(),
                    position.getQuantity()
            );
        }
    }

    public record PositionResponse(List<PositionItemResponse> items) {}
}
