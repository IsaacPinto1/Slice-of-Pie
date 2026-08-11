package com.isaac.sliceofpie.broker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BrokerExceptionHandler {

    @ExceptionHandler(BrokerAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(BrokerAccessDeniedException ex) {
        // Deliberately generic/indistinguishable-from-unmapped-route -
        // see BrokerAccessDeniedException's class comment.
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Not found");
    }

    @ExceptionHandler(BrokerNotConnectedException.class)
    public ProblemDetail handleNotConnected(BrokerNotConnectedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BrokerLookupException.class)
    public ProblemDetail handleLookupError(BrokerLookupException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Failed to reach brokerage provider: " + ex.getMessage());
    }

    @ExceptionHandler(RequestSigningException.class)
    public ProblemDetail handleSigningError(RequestSigningException ex) {
        // Our own fault (bad key config, serialization failure) - not the
        // provider's, and not a client error. Never echo ex.getMessage()
        // here: it can reference key material or payload details that
        // shouldn't leave the server.
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to sign outgoing request");
    }
}
