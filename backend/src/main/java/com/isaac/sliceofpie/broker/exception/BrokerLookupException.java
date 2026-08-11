package com.isaac.sliceofpie.broker.exception;

/**
 * Wraps failures talking to SnapTrade (network errors, non-2xx responses).
 * Kept distinct from BrokerNotConnectedException so callers can tell "we
 * asked and there's nothing there" apart from "we couldn't ask" - same
 * split as InstrumentLookupException/InstrumentNotFoundException.
 */
public class BrokerLookupException extends RuntimeException {

    public BrokerLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
