package com.isaac.sliceofpie.instrument;

import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;
import com.isaac.sliceofpie.instrument.exception.InstrumentNotFoundException;
import com.isaac.sliceofpie.instrument.lookup.InstrumentLookupClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentResolutionServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentLookupClient instrumentLookupClient;

    private InstrumentResolutionService service;

    @BeforeEach
    void setUp() {
        service = new InstrumentResolutionService(instrumentRepository, instrumentLookupClient);
    }

    @Test
    void resolveOrCreate_createsNewInstrument_whenNotAlreadyTracked() {
        InstrumentSearchResult result = new InstrumentSearchResult("AAPL", "APPLE INC");
        when(instrumentLookupClient.search("apple")).thenReturn(List.of(result));
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(instrumentRepository.save(any(Instrument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instrument instrument = service.resolveOrCreate("apple");

        assertThat(instrument.getTicker()).isEqualTo("AAPL");
        assertThat(instrument.getName()).isEqualTo("APPLE INC");
        verify(instrumentRepository).save(any(Instrument.class));
    }

    @Test
    void resolveOrCreate_returnsExistingInstrument_withoutCreatingDuplicate() {
        InstrumentSearchResult result = new InstrumentSearchResult("AAPL", "APPLE INC");
        Instrument existing = new Instrument("AAPL", "APPLE INC", null);

        when(instrumentLookupClient.search("AAPL")).thenReturn(List.of(result));
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(Optional.of(existing));

        Instrument instrument = service.resolveOrCreate("AAPL");

        assertThat(instrument).isSameAs(existing);
        verify(instrumentRepository, never()).save(any());
    }

    @Test
    void resolveOrCreate_throwsNotFound_whenNoResultsReturned() {
        when(instrumentLookupClient.search("zzz_nonexistent")).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveOrCreate("zzz_nonexistent"))
                .isInstanceOf(InstrumentNotFoundException.class);

        verify(instrumentRepository, never()).save(any());
    }

    @Test
    void resolveOrCreate_takesFirstResult_whenMultipleMatchesReturned() {
        InstrumentSearchResult first = new InstrumentSearchResult("AAPL", "APPLE INC");
        InstrumentSearchResult second = new InstrumentSearchResult("APLE", "APPLE HOSPITALITY REIT");

        when(instrumentLookupClient.search("apple")).thenReturn(List.of(first, second));
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(instrumentRepository.save(any(Instrument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instrument instrument = service.resolveOrCreate("apple");

        assertThat(instrument.getTicker()).isEqualTo("AAPL");
        verify(instrumentRepository, never()).findByTicker("APLE");
    }
}