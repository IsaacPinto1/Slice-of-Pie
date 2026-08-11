package com.isaac.sliceofpie.broker.lookup;

import java.util.List;

import com.isaac.sliceofpie.broker.PositionDtos.BrokerHolding;

/**
 * Contract for talking to a brokerage-data provider. Provider-agnostic on
 * purpose, mirroring InstrumentLookupClient/PriceLookupClient's shape -
 * nothing outside this package and its implementations should know
 * SnapTrade specifically is behind it.
 *
 * Personal-key only: no registerUser, no userId/userSecret anywhere in
 * this contract - see the spec's Decision #1. Every implementation must
 * sign requests with just the Personal consumerKey.
 */
public interface BrokerClient {

    /**
     * True if this Personal key currently has at least one connected
     * brokerage account. Backs GET /broker/status and guards
     * PositionService#sync.
     */
    boolean hasConnectedAccounts();

    /**
     * Aggregated holdings for this user, normalized to BrokerHolding. For
     * now there's no storage/retrieval of account ids, since this should
     * stay general across holdings providers, but additional methods
     * might need to be built out to support future capabilities.
     */
    List<BrokerHolding> fetchHoldings();

}
