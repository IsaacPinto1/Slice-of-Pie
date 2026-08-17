package com.isaac.sliceofpie.broker.snaptrade;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw SnapTrade response shapes. This is the only file in the codebase
 * that should know what SnapTrade's JSON actually looks like - everything
 * downstream of SnapTradeAccountClient talks to BrokerClient /
 * PositionDtos.BrokerHolding instead. Only the fields this app currently
 * uses are modeled; everything else SnapTrade returns is ignored.
 */
public class SnapTradeDtos {

    // GET /api/v1/accounts returns a bare JSON array of these.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradeAccount(
            String id,
            String name
    ) {}

    // GET /api/v1/accounts/{accountId}/positions
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradePositionsResponse(
            List<SnapTradePosition> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradePosition(
            SnapTradeInstrument instrument,
            BigDecimal units,
            BigDecimal cost_basis
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradeInstrument(
            String symbol,
            String description
    ) {}
}
