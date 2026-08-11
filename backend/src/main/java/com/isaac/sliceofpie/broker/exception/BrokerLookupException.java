package com.isaac.sliceofpie.broker.exception;

/**
 * Wraps failures talking to the brokerage-data provider (network errors,
 * non-2xx responses). Kept distinct from BrokerNotConnectedException so
 * callers can tell "we asked and there's nothing there" apart from "we
 * couldn't ask" - same split as InstrumentLookupException/
 * InstrumentNotFoundException. Provider-agnostic on purpose - see
 * BrokerClient; a provider-specific client (e.g. SnapTradeAccountClient)
 * throws this rather than a provider-named exception.
 */
public class BrokerLookupException extends RuntimeException {

    public BrokerLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
