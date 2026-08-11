package com.isaac.sliceofpie.broker.lookup;

import java.util.List;

import com.isaac.sliceofpie.broker.BrokerDtos.ClientHoldingResponse;

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
     * Aggregated holdings for this user. For now there's no storage/retrieval
     * of account ids, since this should be general for holdings providers, but
     * additional methods might need to be built out to support future capabilities.
     */
    List<ClientHoldingResponse> fetchHoldings();

}
