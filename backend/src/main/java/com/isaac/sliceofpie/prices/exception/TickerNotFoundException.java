package com.isaac.sliceofpie.prices.exception;

/* Used when price lookup can't find a given ticker or fails*/
public class TickerNotFoundException extends RuntimeException {
    
    public TickerNotFoundException(String message, Throwable cause){
        super(message, cause);
    }

    public TickerNotFoundException(String message){
        super(message);
    }
}
