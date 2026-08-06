package com.isaac.sliceofpie.price;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.exception.InvalidPriceException;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;

@ExtendWith(MockitoExtension.class)
public class PriceServiceTest {

    @Mock
    PriceLookupClient priceLookupClient;

    @BeforeEach
    void setUp(){
    }

    @Test
    void tickersearch_returnsexpectedprice() {
        BigDecimal price = new BigDecimal(158);
        Mockito.when(priceLookupClient.getPrice("PLTR")).thenReturn(new PriceResponse(price));

        PriceResponse response = priceLookupClient.getPrice("PLTR");
        assertEquals(response.price(), price);
    }

    @Test
    void tickersearch_throwsexception_fornullprice() {
        Mockito.when(priceLookupClient.getPrice("PLTR")).thenReturn(new PriceResponse(null));

        assertThatThrownBy(()->priceLookupClient.getPrice("PLTR"))
        .isInstanceOf(InvalidPriceException.class)
        .hasMessageContaining("PLTR");
    }
    
    @Test
    void tickersearch_throwsexception_fornegative() {
        Mockito.when(priceLookupClient.getPrice("PLTR")).thenReturn(PriceResponse.from(-1));

        assertThatThrownBy(()->priceLookupClient.getPrice("PLTR"))
        .isInstanceOf(InvalidPriceException.class)
        .hasMessageContaining("PLTR");
    }
}
