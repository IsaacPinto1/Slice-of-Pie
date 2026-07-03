package com.isaac.sliceofpie.instrument.exception;

public class InstrumentNotFoundException extends RuntimeException {

    public InstrumentNotFoundException(String query) {
        super("No instrument found for query: " + query);
    }
}