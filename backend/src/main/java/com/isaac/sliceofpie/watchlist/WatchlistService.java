package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final InstrumentResolutionService instrumentResolutionService;

    public WatchlistService(WatchlistRepository watchlistRepository,
                             InstrumentResolutionService instrumentResolutionService) {
        this.watchlistRepository = watchlistRepository;
        this.instrumentResolutionService = instrumentResolutionService;
    }

    /**
     * Resolves the given ticker/query to an Instrument (creating it if new)
     * and adds it to the user's watchlist. Following something already on
     * the watchlist is a no-op, not an error.
     */
    @Transactional
    public WatchlistItem follow(Long userId, String watchlistAddQuery) {
        Instrument instrument = instrumentResolutionService.resolveOrCreate(watchlistAddQuery);

        Optional<WatchlistItem> existing =
                watchlistRepository.findByUserIdAndInstrumentId(userId, instrument.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return watchlistRepository.save(new WatchlistItem(userId, instrument));
        } catch (DataIntegrityViolationException e) {
            // Concurrent follow of the same instrument - return the winner instead of failing.
            return watchlistRepository.findByUserIdAndInstrumentId(userId, instrument.getId())
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Unfollows by ticker. Looks up the instrument WITHOUT creating it -
     * unfollowing something that was never followed (or never resolved)
     * should be a silent no-op, not a Finnhub call that creates a new
     * Instrument row as a side effect of a delete.
     */
    @Transactional
    public void unfollow(Long userId, Long instrumentId) {
        Long idToDelete = instrumentResolutionService.getById(instrumentId).getId();
        watchlistRepository.deleteByUserIdAndInstrumentId(userId, idToDelete);
    }

    public List<WatchlistItem> listForUser(Long userId) {
        return watchlistRepository.findAllByUserIdFetchInstrument(userId);
    }
}