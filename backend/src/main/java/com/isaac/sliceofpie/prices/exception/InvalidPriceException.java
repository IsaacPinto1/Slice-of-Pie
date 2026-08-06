package com.isaac.sliceofpie.prices.exception;

/* Used when the retrieved price is invalid (null or negative) */
public class InvalidPriceException extends RuntimeException{
    
    public InvalidPriceException(String message){
        super(message);
    }
}
