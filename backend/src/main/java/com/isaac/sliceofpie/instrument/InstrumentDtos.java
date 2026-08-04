package com.isaac.sliceofpie.instrument;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public class InstrumentDtos {
    // Sent once the user has selected a result from the search dropdown -
    // ticker and name both come from that result, not a free-text query.
    public record CreateInstrumentRequest(
        @NotBlank(message = "ticker must not be blank") String ticker,
        @NotBlank(message = "name must not be blank") String name
    ) {}

    public record InstrumentResponse(
        Long id,
        String ticker,
        String name,
        String exchange,
        Instant createdAt
    ) {
        public static InstrumentResponse from(Instrument instrument) {
            return new InstrumentResponse(
                    instrument.getId(),
                    instrument.getTicker(),
                    instrument.getName(),
                    instrument.getExchange(),
                    instrument.getCreatedAt()
            );
        }
    }

    public record InstrumentSearchResult(
        String ticker,
        String name
    ) {}
}
