package com.isaac.sliceofpie.thesis;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThesisService {

    private final ThesisRepository thesisRepository;
    private final InstrumentResolutionService instrumentResolutionService;

    public ThesisService(ThesisRepository thesisRepository,
                          InstrumentResolutionService instrumentResolutionService) {
        this.thesisRepository = thesisRepository;
        this.instrumentResolutionService = instrumentResolutionService;
    }

    /**
     * Creates or updates (upsert) the user's thesis for a ticker. Resolving with
     * getById enforces that the instrument must exist, returning an exception otherwise.
     */
    @Transactional
    public Thesis upsert(Long userId, Long instrumentId, String content) {
        Instrument instrument = instrumentResolutionService.getById(instrumentId);

        return thesisRepository.findByUserIdAndInstrumentId(userId, instrument.getId())
                .map(existing -> {
                    existing.setContent(content);
                    return existing; // dirty-checked on commit (Hibernate Transactional), no explicit save needed
                })
                .orElseGet(() -> thesisRepository.save(new Thesis(userId, instrument, content)));
    }

    /**
     * Thesis doesn't need to exist for a given user/instrument pair. 
     */
    public Optional<Thesis> getByInstrumentId(Long userId, Long id) {
        return thesisRepository.findByUserIdAndInstrumentId(userId, id);
    }
}