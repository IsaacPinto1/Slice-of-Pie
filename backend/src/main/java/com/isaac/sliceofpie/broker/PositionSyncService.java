package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.broker.exception.BrokerNotConnectedException;
import com.isaac.sliceofpie.broker.lookup.BrokerClient;
import com.isaac.sliceofpie.broker.lookup.BrokerClient.SnapTradeHolding;
import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PositionSyncService {

    private static final Logger log = LoggerFactory.getLogger(PositionSyncService.class);

    private final BrokerClient snapTradeClient;
    private final InstrumentResolutionService instrumentResolutionService;
    private final PositionRepository positionRepository;

    public PositionSyncService(BrokerClient snapTradeClient,
                                InstrumentResolutionService instrumentResolutionService,
                                PositionRepository positionRepository) {
        this.snapTradeClient = snapTradeClient;
        this.instrumentResolutionService = instrumentResolutionService;
        this.positionRepository = positionRepository;
    }

    /**
     * Full reconciliation sync (decision #7 - a diff, not just an upsert):
     * upserts a Position for every holding SnapTrade currently reports,
     * then deletes any local Position for this user whose instrument isn't
     * in that response.
     *
     * userId is still the APP's user id, for scoping Position rows per the
     * existing per-user data model - see the Single-identity caveat for
     * what this does and doesn't isolate; it does not mean userId's
     * SnapTrade holdings, since there's only ever one real SnapTrade
     * identity behind this Personal key.
     */
    @Transactional
    public List<Position> sync(Long userId) {
        if (!snapTradeClient.hasConnectedAccounts()) {
            throw new BrokerNotConnectedException();
        }

        List<SnapTradeHolding> holdings = snapTradeClient.fetchHoldings();
        List<Long> resolvedInstrumentIds = new ArrayList<>();

        for (SnapTradeHolding holding : holdings) {
            try {
                resolvedInstrumentIds.add(upsert(userId, holding));
            } catch (Exception e) {
                // One malformed/unresolvable holding (an instrument type
                // this app doesn't model well, a ticker that won't
                // resolve, etc.) shouldn't abort the rest of the batch -
                // same "one bad item" isolation pattern as
                // PriceRefreshScheduler#refreshDueInstruments. Skipped
                // silently, no per-item warning surfaced to the user, per
                // product decision.
                log.warn("Skipping unresolvable SnapTrade holding '{}' for user {}: {}",
                        holding.ticker(), userId, e.getMessage());
            }
        }

        // Spring Data renders "instrument_id NOT IN ()" for an empty
        // collection, which most drivers reject - fall back to deleting
        // everything for this user when nothing resolved this sync.
        if (resolvedInstrumentIds.isEmpty()) {
            positionRepository.deleteByUserId(userId);
        } else {
            positionRepository.deleteByUserIdAndInstrumentIdNotIn(userId, resolvedInstrumentIds);
        }

        return positionRepository.findAllByUserIdFetchInstrument(userId);
    }

    private Long upsert(Long userId, SnapTradeHolding holding) {
        Instrument instrument = resolveInstrument(holding);

        Optional<Position> existing =
                positionRepository.findByUserIdAndInstrumentId(userId, instrument.getId());
        if (existing.isPresent()) {
            existing.get().setQuantity(holding.quantity());
        } else {
            positionRepository.save(new Position(userId, instrument, holding.quantity()));
        }
        return instrument.getId();
    }

    // Reuses the same resolution path everything else in the app goes
    // through (InstrumentController#create / WatchlistService#follow):
    // resolve() first so an already-known ticker doesn't trigger a
    // redundant create(), falling back to create() - using SnapTrade's own
    // holding name, never inventing one - only when genuinely new.
    private Instrument resolveInstrument(SnapTradeHolding holding) {
        try {
            return instrumentResolutionService.resolve(holding.ticker());
        } catch (RuntimeException notFound) {
            String name = (holding.name() == null || holding.name().isBlank())
                    ? holding.ticker()
                    : holding.name();
            return instrumentResolutionService.create(holding.ticker(), name);
        }
    }
}
