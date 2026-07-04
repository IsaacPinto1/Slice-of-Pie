package com.isaac.sliceofpie.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    Optional<WatchlistItem> findByUserIdAndInstrumentId(Long userId, Long instrumentId);

    // fetch the instrument eagerly so listing the watchlist doesn't take N+1 queries (1 for each watchlist, 1 extra for each instrument)
    @Query("select w from WatchlistItem w join fetch w.instrument where w.userId = :userId")
    List<WatchlistItem> findAllByUserIdFetchInstrument(Long userId);

    void deleteByUserIdAndInstrumentId(Long userId, Long instrumentId);
}