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

    private final InstrumentRepository instrumentRepository;
    private final InstrumentLookupClient instrumentLookupClient;

    public InstrumentResolutionService(InstrumentRepository instrumentRepository,
                                        InstrumentLookupClient instrumentLookupClient) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentLookupClient = instrumentLookupClient;
    }

    /**
     * Resolves a free-text query (ticker or company name) to a durable Instrument,
     * creating one if this is the first time we've seen it.
     *
     * NOTE: for now this just takes the FIRST result the lookup provider returns -
     * no disambiguation UI yet. See roadmap "open decisions" for follow-up.
     */
    @Transactional
    public Instrument resolveOrCreate(String query) {
        String normalizedQuery = query.trim();

        List<InstrumentSearchResult> results = instrumentLookupClient.search(normalizedQuery);

        if (results == null || results.isEmpty()) {
            throw new InstrumentNotFoundException(normalizedQuery);
        }

        InstrumentSearchResult firstResult = results.get(0);
        String ticker = firstResult.ticker();

        return instrumentRepository.findByTicker(ticker)
                .orElseGet(() -> createInstrument(ticker, firstResult.name()));
    }

    public Instrument getById(Long instrumentId) {
        System.out.println("LOOKINGFOR" + instrumentId);
        return instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new InstrumentNotFoundException("id=" + instrumentId));
    }

    public List<String> getTickersFromIds(List<Long> ids){
        return ids.stream().map(id -> getById(id).getTicker()).toList();
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