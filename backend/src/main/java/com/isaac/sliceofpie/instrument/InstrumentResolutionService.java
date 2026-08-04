package com.isaac.sliceofpie.instrument;

import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;
import com.isaac.sliceofpie.instrument.exception.InstrumentNotFoundException;
import com.isaac.sliceofpie.instrument.lookup.InstrumentLookupClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrumentResolutionService {

    // The dropdown shows all of these, scrolled to a 5-row viewport - the
    // cap just bounds how much we ask the provider for and send over the
    // wire per keystroke.
    private static final int MAX_SEARCH_RESULTS = 10;

    private final InstrumentRepository instrumentRepository;
    private final InstrumentLookupClient instrumentLookupClient;

    public InstrumentResolutionService(InstrumentRepository instrumentRepository,
                                        InstrumentLookupClient instrumentLookupClient) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentLookupClient = instrumentLookupClient;
    }

    /**
     * Read-only lookup for the search-as-you-type dropdown. Never touches
     * the database - just proxies the provider's candidates, capped to
     * MAX_SEARCH_RESULTS. Nothing is created here.
     */
    public List<InstrumentSearchResult> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        List<InstrumentSearchResult> results = instrumentLookupClient.search(normalizedQuery);
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        return results.size() > MAX_SEARCH_RESULTS
                ? results.subList(0, MAX_SEARCH_RESULTS)
                : results;
    }

    /**
     * Creates a durable Instrument from a result the user explicitly picked
     * out of the search dropdown (ticker + name both come from that result -
     * no further provider call needed). Idempotent: following up with the
     * same ticker returns the existing row instead of duplicating it.
     *
     * This is the ONLY path that creates an Instrument. Everything else
     * (watchlist follow, positions, theses) must go through resolve()
     * instead, which never creates.
     */
    @Transactional
    public Instrument create(String ticker, String name) {
        String normalizedTicker = ticker.trim().toUpperCase();

        return instrumentRepository.findByTicker(normalizedTicker)
                .orElseGet(() -> createInstrument(normalizedTicker, name.trim()));
    }

    /**
     * Looks up an already-known ticker WITHOUT creating anything. Instruments
     * only ever come into existence via create() (search -> select -> create),
     * so anything that isn't found here genuinely hasn't been added yet.
     */
    public Instrument resolve(String ticker) {
        String normalizedTicker = ticker.trim().toUpperCase();
        return instrumentRepository.findByTicker(normalizedTicker)
                .orElseThrow(() -> new InstrumentNotFoundException(normalizedTicker));
    }

    public Instrument getById(Long instrumentId) {
        return instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new InstrumentNotFoundException("id=" + instrumentId));
    }

    private Instrument createInstrument(String ticker, String name) {
        try {
            Instrument instrument = new Instrument(ticker, name, null);
            return instrumentRepository.save(instrument);
        } catch (DataIntegrityViolationException e) {
            // Another request resolved the same ticker concurrently (unique
            // constraint tripped) - fetch the winner instead of failing.
            return instrumentRepository.findByTicker(ticker)
                    .orElseThrow(() -> e);
        }
    }
}