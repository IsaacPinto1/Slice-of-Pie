package com.isaac.sliceofpie.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.isaac.sliceofpie.instrument.InstrumentResolutionService;
import com.isaac.sliceofpie.prices.PriceDtos.PriceValueResponse;
import com.isaac.sliceofpie.prices.PriceService;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;

/**
 * Covers PriceService.tryFetchPrice() - the non-transactional, never-throws
 * fetch InstrumentResolutionService.createInstrument() uses to populate a
 * real price on the Instrument it's about to return, synchronously, in the
 * same transaction/persistence context.
 */
@ExtendWith(MockitoExtension.class)
class PriceServiceTryFetchPriceTest {

    @Mock
    private PriceLookupClient priceLookupClient;

    @Mock
    private InstrumentResolutionService instrumentResolutionService;

    private PriceService priceService;

    @Test
    void tryFetchPrice_returnsThePrice_whenLookupSucceeds() {
        priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        when(priceLookupClient.getPrice("AAPL")).thenReturn(new PriceValueResponse(new BigDecimal("158")));

        Optional<BigDecimal> result = priceService.tryFetchPrice("AAPL");

        assertThat(result).contains(new BigDecimal("158"));
    }

    @Test
    void tryFetchPrice_returnsEmpty_whenPriceIsNull() {
        priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        when(priceLookupClient.getPrice("BADTICKER")).thenReturn(new PriceValueResponse(null));

        Optional<BigDecimal> result = priceService.tryFetchPrice("BADTICKER");

        assertThat(result).isEmpty();
    }

    @Test
    void tryFetchPrice_returnsEmpty_whenPriceIsNegative() {
        priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        when(priceLookupClient.getPrice("BADTICKER")).thenReturn(new PriceValueResponse(new BigDecimal("-1")));

        Optional<BigDecimal> result = priceService.tryFetchPrice("BADTICKER");

        assertThat(result).isEmpty();
    }

    @Test
    void tryFetchPrice_returnsEmpty_neverThrows_whenLookupClientThrows() {
        priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        when(priceLookupClient.getPrice("AAPL")).thenThrow(new RuntimeException("provider unavailable"));

        Optional<BigDecimal> result = priceService.tryFetchPrice("AAPL");

        assertThat(result).isEmpty();
    }
}
