package com.isaac.sliceofpie.prices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PriceExceptionHandler {

    @ExceptionHandler(PriceNotFoundException.class)
    public ProblemDetail handleNotFound(PriceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TickerNotFoundException.class)
    public ProblemDetail handleLookupError(TickerNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Failed to reach price lookup provider: " + ex.getMessage());
    }
}