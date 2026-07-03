package com.isaac.sliceofpie.instrument;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public class InstrumentDtos {
    public record ResolveInstrumentRequest(@NotBlank(message = "query must not be blank") String query) {}

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
