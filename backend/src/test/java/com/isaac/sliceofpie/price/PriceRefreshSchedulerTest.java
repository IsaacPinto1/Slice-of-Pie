package com.isaac.sliceofpie.price;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.isaac.sliceofpie.instrument.InstrumentRepository;
import com.isaac.sliceofpie.prices.PriceRefreshScheduler;
import com.isaac.sliceofpie.prices.PriceService;
import com.isaac.sliceofpie.prices.exception.InvalidPriceException;

/**
 * Unit-level test for PriceRefreshScheduler's own logic: which slot it asks
 * the repository for, and that one instrument failing doesn't stop the rest
 * of the batch. It does NOT assert on wall-clock slot selection - which
 * slot is "due" right now is a thin, time-dependent one-liner better left
 * unmocked than pinned to a specific minute; what matters here is that
 * whatever slot the repository returns for gets iterated correctly.
 */
@ExtendWith(MockitoExtension.class)
class PriceRefreshSchedulerTest {

    @Mock
    InstrumentRepository instrumentRepository;

    @Mock
    PriceService priceService;

    @Test
    void refreshDueInstruments_refreshesEveryInstrumentInTheDueSlot() {
        when(instrumentRepository.findIdsByIdModulo(eq(PriceRefreshScheduler.REFRESH_WINDOW_MINUTES), anyInt()))
                .thenReturn(List.of(1L, 2L, 3L));

        new PriceRefreshScheduler(instrumentRepository, priceService).refreshDueInstruments();

        verify(priceService, times(1)).forceLatestPrice(1L);
        verify(priceService, times(1)).forceLatestPrice(2L);
        verify(priceService, times(1)).forceLatestPrice(3L);
    }

    @Test
    void refreshDueInstruments_oneFailureDoesNotStopTheRestOfTheBatch() {
        when(instrumentRepository.findIdsByIdModulo(eq(PriceRefreshScheduler.REFRESH_WINDOW_MINUTES), anyInt()))
                .thenReturn(List.of(1L, 2L, 3L));
        when(priceService.forceLatestPrice(2L))
                .thenThrow(new InvalidPriceException("Retrieved price invalid for ticker 'BAD'"));

        new PriceRefreshScheduler(instrumentRepository, priceService).refreshDueInstruments();

        verify(priceService, times(1)).forceLatestPrice(1L);
        verify(priceService, times(1)).forceLatestPrice(2L);
        verify(priceService, times(1)).forceLatestPrice(3L);
    }

    @Test
    void refreshDueInstruments_noDueInstruments_callsPriceServiceZeroTimes() {
        when(instrumentRepository.findIdsByIdModulo(eq(PriceRefreshScheduler.REFRESH_WINDOW_MINUTES), anyInt()))
                .thenReturn(List.of());

        new PriceRefreshScheduler(instrumentRepository, priceService).refreshDueInstruments();

        verify(priceService, never()).forceLatestPrice(anyLong());
    }
}
