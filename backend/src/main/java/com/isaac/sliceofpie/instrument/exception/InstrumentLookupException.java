package com.isaac.sliceofpie.instrument.exception;
 
/**
 * Wraps failures talking to the instrument lookup provider (rate limits,
 * timeouts, non-2xx responses). Kept distinct from InstrumentNotFoundException
 * so callers can tell "we asked and there's nothing there" apart from
 * "we couldn't ask". Provider-agnostic on purpose - see InstrumentLookupClient.
 */
public class InstrumentLookupException extends RuntimeException {
 
    public InstrumentLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}