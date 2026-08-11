package com.isaac.sliceofpie.broker.lookup;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contract for talking to SnapTrade. Provider-agnostic on purpose, mirroring
 * InstrumentLookupClient/PriceLookupClient's shape - nothing outside this
 * package and its implementations should know SnapTrade specifically is
 * behind it.
 *
 * Personal-key only: no registerUser, no userId/userSecret anywhere in
 * this contract - see the spec's Decision #1. Every implementation must
 * sign requests with just the Personal consumerKey.
 */
public interface PositionsClient {

    /**
     * True if this Personal key currently has at least one connected
     * brokerage account. Backs GET /broker/snaptrade/status and guards
     * PositionSyncService#sync.
     */
    boolean hasConnectedAccounts();

    /**
     * Aggregated holdings across every connected account under this
     * Personal key - one flat list, no per-account breakdown (decision #5
     * in the spec).
     */
    List<SnapTradeHolding> fetchHoldings();

    record SnapTradeHolding(String ticker, String name, BigDecimal quantity) {}
}
