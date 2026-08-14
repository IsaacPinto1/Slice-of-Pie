package com.isaac.sliceofpie.price;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentCreatedEvent;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;
import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.PriceService;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;

/**
 * Covers PriceService.onInstrumentCreated() - the AFTER_COMMIT listener that
 * gives a freshly-created Instrument a real initial price (see
 * InstrumentResolutionService.createInstrument()), instead of leaving it at
 * price=0 until PriceRefreshScheduler's next tick.
 */
@ExtendWith(MockitoExtension.class)
class PriceServiceInstrumentCreatedListenerTest {

    @Mock
    private PriceLookupClient priceLookupClient;

    @Mock
    private InstrumentResolutionService instrumentResolutionService;

    private PriceService priceService;

    @Test
    void onInstrumentCreated_fetchesAndSetsThePriceForTheNewInstrument() {
        priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        Instrument instrument = new Instrument("AAPL", "APPLE INC", null);
        when(instrumentResolutionService.getById(42L)).thenReturn(instrument);
        when(priceLookupClient.getPrice("AAPL")).thenReturn(new PriceResponse(new BigDecimal("158")));

        priceService.onInstrumentCreated(new InstrumentCreatedEvent(42L));

        org.assertj.core.api.Assertions.assertThat(instrument.getPrice()).isEqualTo(158.0);
        org.assertj.core.api.Assertions.assertThat(instrument.getPriceUpdatedAt()).isNotNull();
    }

    @Test
    void onInstrumentCreated_swallowsFailure_soItNeverPropagatesPastTheListener() {
        priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        Instrument instrument = new Instrument("BADTICKER", "Unknown Co", null);
        when(instrumentResolutionService.getById(7L)).thenReturn(instrument);
        // Simulates the provider failing/returning nothing usable - this
        // must NOT escape the listener, since it fires after the creating
        // transaction has already committed.
        when(priceLookupClient.getPrice("BADTICKER")).thenReturn(new PriceResponse(null));

        assertThatCode(() -> priceService.onInstrumentCreated(new InstrumentCreatedEvent(7L)))
                .doesNotThrowAnyException();
    }

    @Test
    void onInstrumentCreated_swallowsUnexpectedExceptions_fromDownstreamLookups() {
        priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        when(instrumentResolutionService.getById(99L))
                .thenThrow(new RuntimeException("instrument vanished mid-flight"));

        assertThatCode(() -> priceService.onInstrumentCreated(new InstrumentCreatedEvent(99L)))
                .doesNotThrowAnyException();
    }
}
