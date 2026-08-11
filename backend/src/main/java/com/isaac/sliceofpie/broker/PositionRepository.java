package com.isaac.sliceofpie.broker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByUserIdAndInstrumentId(Long userId, Long instrumentId);

    // fetch the instrument eagerly, same N+1-avoidance reasoning as
    // WatchlistRepository#findAllByUserIdFetchInstrument.
    @Query("select p from Position p join fetch p.instrument where p.userId = :userId")
    List<Position> findAllByUserIdFetchInstrument(Long userId);

    // Reconciliation step of a sync (decision #7 - full reconciliation, not
    // just an upsert): deletes anything for this user whose instrument
    // isn't in the latest SnapTrade response. NOTE: callers must NOT call
    // this with an empty instrumentIds list - Spring Data renders "NOT IN
    // ()" for an empty collection, which most drivers reject. Use
    // deleteByUserId instead when nothing resolved.
    void deleteByUserIdAndInstrumentIdNotIn(Long userId, List<Long> instrumentIds);

    void deleteByUserId(Long userId);
}
