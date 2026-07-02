package com.isaac.sliceofpie.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUserIdOrderByTickerAsc(Long userId);

    boolean existsByUserIdAndTicker(Long userId, String ticker);

    void deleteByUserIdAndTicker(Long userId, String ticker);
}