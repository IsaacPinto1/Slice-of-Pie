package com.isaac.sliceofpie.thesis;

import java.time.Instant;

public class ThesisDtos {

    public record UpsertThesisRequest(String ticker, String content) {}

    public record ThesisResponse(
            String ticker,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {}
}