package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.broker.PositionDtos.BrokerHolding;
import com.isaac.sliceofpie.broker.exception.BrokerNotConnectedException;
import com.isaac.sliceofpie.broker.lookup.BrokerClient;
import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Single service for the whole broker/positions feature: connection
// status, reading a user's stored positions, and syncing them from
// whichever BrokerClient is wired up.
@Service
public class PositionService {

    private static final Logger log = LoggerFactory.getLogger(PositionService.class);

    private final BrokerClient brokerClient;
    private final InstrumentResolutionService instrumentResolutionService;
    private final PositionRepository positionRepository;

    public PositionService(BrokerClient brokerClient,
                            InstrumentResolutionService instrumentResolutionService,
                            PositionRepository positionRepository) {
        this.brokerClient = brokerClient;
        this.instrumentResolutionService = instrumentResolutionService;
        this.positionRepository = positionRepository;
    }

    /**
     * Live call to the provider every time - with a Personal key there's no
     * local BrokerConnection row to check against; "connected" is purely a
     * question the provider itself can answer.
     * 
     * TODO: Can be cached potentially in future.
     */
    public boolean hasConnections() {
        return brokerClient.hasConnectedAccounts();
    }

    public List<Position> listForUser(Long userId) {
        return positionRepository.findAllByUserIdFetchInstrument(userId);
    }

    /**
     * Full reconciliation sync: upserts a Position for every holding the provider currently reports,
     * then deletes any local Position for this user whose instrument isn't
     * in that response.
     *
     * userId is still the APP's user id, for scoping Position rows per the
     * existing per-user data model - see BrokerAccessGuard's Single-identity
     * caveat for what this does and doesn't isolate; it does not mean
     * userId's own brokerage holdings, since there's only ever one real
     * provider identity behind this Personal key.
     */
    @Transactional
    public List<Position> sync(Long userId) {
        if (!brokerClient.hasConnectedAccounts()) {
            throw new BrokerNotConnectedException();
        }

        List<BrokerHolding> holdings = brokerClient.fetchHoldings(); // Pulls actual positions
        List<Long> resolvedInstrumentIds = new ArrayList<>();

        for (BrokerHolding holding : holdings) {
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
                log.warn("Skipping unresolvable holding '{}' for user {}: {}",
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

    private Long upsert(Long userId, BrokerHolding holding) {
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
    // redundant create(), falling back to create() - using the provider's
    // own holding name, never inventing one - only when genuinely new.
    private Instrument resolveInstrument(BrokerHolding holding) {
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
