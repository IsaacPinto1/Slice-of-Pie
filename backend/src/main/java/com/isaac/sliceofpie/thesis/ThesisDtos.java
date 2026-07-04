package com.isaac.sliceofpie.thesis;

import java.time.Instant;

public class ThesisDtos {

    public record UpsertThesisRequest(
            Long instrumentId,
            String content
    ) {}

    public record ThesisResponse(
            Long instrumentId,
            String ticker,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ThesisResponse from(Thesis thesis) {
            return new ThesisResponse(
                    thesis.getInstrument().getId(),
                    thesis.getInstrument().getTicker(),
                    thesis.getContent(),
                    thesis.getCreatedAt(),
                    thesis.getUpdatedAt()
            );
        }
    }
}