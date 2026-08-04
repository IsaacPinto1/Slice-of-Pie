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

    // ---------- search() - read-only, never touches the database ----------

    @Test
    void search_returnsProviderResults_cappedAtMaxSearchResults() {
        // One more than the cap, generated dynamically so this test keeps
        // proving the boundary even if MAX_SEARCH_RESULTS changes.
        List<InstrumentSearchResult> oversizedResults = java.util.stream.IntStream
                .range(0, InstrumentResolutionService.MAX_SEARCH_RESULTS + 1)
                .mapToObj(i -> new InstrumentSearchResult("SYM" + i, "COMPANY " + i))
                .toList();
        when(instrumentLookupClient.search("apple")).thenReturn(oversizedResults);

        List<InstrumentSearchResult> results = service.search("apple");

        assertThat(results).hasSize(InstrumentResolutionService.MAX_SEARCH_RESULTS);
        assertThat(results).containsExactlyElementsOf(
                oversizedResults.subList(0, InstrumentResolutionService.MAX_SEARCH_RESULTS));
        verifyNoInteractions(instrumentRepository);
    }

    @Test
    void search_returnsAllResults_whenFewerThanMaxSearchResults() {
        // One fewer than the cap, generated dynamically for the same reason.
        List<InstrumentSearchResult> underCapResults = java.util.stream.IntStream
                .range(0, InstrumentResolutionService.MAX_SEARCH_RESULTS - 1)
                .mapToObj(i -> new InstrumentSearchResult("SYM" + i, "COMPANY " + i))
                .toList();
        when(instrumentLookupClient.search("apple")).thenReturn(underCapResults);

        List<InstrumentSearchResult> results = service.search("apple");

        assertThat(results).hasSize(InstrumentResolutionService.MAX_SEARCH_RESULTS - 1);
        assertThat(results).containsExactlyElementsOf(underCapResults);
    }

    @Test
    void search_returnsEmptyList_forBlankQuery() {
        List<InstrumentSearchResult> results = service.search("   ");

        assertThat(results).isEmpty();
        verifyNoInteractions(instrumentLookupClient);
    }

    @Test
    void search_returnsEmptyList_whenProviderReturnsNothing() {
        when(instrumentLookupClient.search("zzz_nonexistent")).thenReturn(List.of());

        List<InstrumentSearchResult> results = service.search("zzz_nonexistent");

        assertThat(results).isEmpty();
    }

    // ---------- create() - the only path that creates an Instrument ----------

    @Test
    void create_createsNewInstrument_fromExplicitTickerAndName() {
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(instrumentRepository.save(any(Instrument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instrument instrument = service.create("AAPL", "APPLE INC");

        assertThat(instrument.getTicker()).isEqualTo("AAPL");
        assertThat(instrument.getName()).isEqualTo("APPLE INC");
        verify(instrumentRepository).save(any(Instrument.class));
        verifyNoInteractions(instrumentLookupClient);
    }

    @Test
    void create_returnsExistingInstrument_withoutCreatingDuplicate() {
        Instrument existing = new Instrument("AAPL", "APPLE INC", null);
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(Optional.of(existing));

        Instrument instrument = service.create("AAPL", "APPLE INC");

        assertThat(instrument).isSameAs(existing);
        verify(instrumentRepository, never()).save(any());
    }

    @Test
    void create_normalizesTickerToUppercase() {
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(instrumentRepository.save(any(Instrument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create("aapl", "APPLE INC");

        verify(instrumentRepository).findByTicker("AAPL");
    }

    // ---------- resolve() - lookup-only, never creates ----------

    @Test
    void resolve_returnsExistingInstrument() {
        Instrument existing = new Instrument("AAPL", "APPLE INC", null);
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(Optional.of(existing));

        Instrument instrument = service.resolve("AAPL");

        assertThat(instrument).isSameAs(existing);
        verifyNoInteractions(instrumentLookupClient);
    }

    @Test
    void resolve_throwsNotFound_andNeverCreates_whenTickerUnknown() {
        when(instrumentRepository.findByTicker("ZZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("ZZZZ"))
                .isInstanceOf(InstrumentNotFoundException.class);

        verify(instrumentRepository, never()).save(any());
        verifyNoInteractions(instrumentLookupClient);
    }
}
