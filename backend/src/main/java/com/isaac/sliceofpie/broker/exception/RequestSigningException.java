package com.isaac.sliceofpie.broker.exception;

/**
 * Thrown when a BrokerClient implementation fails to sign an outgoing
 * request (bad key material, unsupported algorithm, a payload that can't
 * be serialized for signing). Provider-agnostic on purpose - see
 * BrokerClient - whichever signing scheme a given provider uses should
 * throw this, not a provider-named exception like "SnapTradeSigningException".
 */
public class RequestSigningException extends RuntimeException {

    public RequestSigningException(String message, Throwable cause) {
        super(message, cause);
    }
}
