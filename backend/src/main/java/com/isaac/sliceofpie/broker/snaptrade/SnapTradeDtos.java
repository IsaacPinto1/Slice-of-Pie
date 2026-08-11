package com.isaac.sliceofpie.broker.snaptrade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Best-effort mapping of SnapTrade's /accounts and
 * /accounts/{id}/positions response shapes, based on their public API
 * docs. NOT verified against a live call in this sandbox (no network
 * access to SnapTrade here) - if positions come back with nulls where
 * they shouldn't, check the actual response shape against
 * https://docs.snaptrade.com/reference/Accounts and adjust field names
 * here. @JsonIgnoreProperties(ignoreUnknown = true) everywhere so any
 * extra fields SnapTrade sends don't break deserialization either way.
 */
public class SnapTradeDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradeAccount(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradeSymbol(String symbol, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradeUniversalSymbol(SnapTradeSymbol symbol, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapTradePosition(SnapTradeUniversalSymbol symbol, BigDecimal units) {}
}
