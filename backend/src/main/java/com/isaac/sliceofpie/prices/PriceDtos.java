package com.isaac.sliceofpie.prices;

import java.math.BigDecimal;

public class PriceDtos {
    
    public record PriceResponse(BigDecimal price){
        public static PriceResponse from(int number){
            BigDecimal bigPrice = new BigDecimal(number);
            return new PriceResponse(bigPrice);
        }
    }
}
