package com.isaac.sliceofpie.broker.exception;

/**
 * Thrown when the brokerage provider reports zero accounts under the
 * Personal key. Renamed from the v1 spec's BrokerConnectionNotFoundException -
 * this is no longer about a missing local DB row, it's about the provider
 * itself reporting no connections. The frontend shouldn't offer a sync
 * button in this state, but the API must not assume that's respected.
 */
public class BrokerNotConnectedException extends RuntimeException {

    public BrokerNotConnectedException() {
        super("No brokerage accounts connected under this provider key");
    }
}
