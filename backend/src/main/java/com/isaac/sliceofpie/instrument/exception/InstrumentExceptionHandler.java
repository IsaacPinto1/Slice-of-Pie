package com.isaac.sliceofpie.instrument.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InstrumentExceptionHandler {

    @ExceptionHandler(InstrumentNotFoundException.class)
    public ProblemDetail handleNotFound(InstrumentNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InstrumentLookupException.class)
    public ProblemDetail handleLookupError(InstrumentLookupException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Failed to reach instrument lookup provider: " + ex.getMessage());
    }
}