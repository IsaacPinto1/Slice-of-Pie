package com.isaac.sliceofpie.broker;

import java.math.BigDecimal;

public class BrokerDtos {

    public record BrokerStatusResponse(boolean connected) {}

    public record ClientHoldingResponse(String ticker, String name, BigDecimal quantity) {}
}
