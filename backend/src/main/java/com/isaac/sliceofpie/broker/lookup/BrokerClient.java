package com.isaac.sliceofpie.broker.lookup;

import java.util.List;

import com.isaac.sliceofpie.broker.PositionDtos.BrokerHolding;

/**
 * Contract for talking to a brokerage-data provider. Provider-agnostic on
 * purpose, mirroring InstrumentLookupClient/PriceLookupClient's shape -
 * nothing outside this package and its implementations should know
 * SnapTrade specifically is behind it.
 */
public interface BrokerClient {

    /*
    * Method to check that positions can be pulled for the user. Current implementation
    * stores credentials as env vars for personal use, might need to be updated to take
    * user info later on
     */
    boolean hasConnectedAccounts();

    /**
     * Aggregated holdings for this user, normalized to BrokerHolding. As above,
     * current implementation requires no input because of env vars, but this might
     * change if ever used for more than one user (no plans currently)
     */
    List<BrokerHolding> fetchHoldings();

}
