package com.isaac.sliceofpie.watchlist;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    public WatchlistService(WatchlistRepository watchlistRepository) {
        this.watchlistRepository = watchlistRepository;
    }

    public String follow(Long userId, String ticker) {
        String normalized = ticker.toUpperCase();
        if (!watchlistRepository.existsByUserIdAndTicker(userId, normalized)) {
            watchlistRepository.save(new WatchlistItem(userId, normalized));
        }
        return normalized;
    }

    @Transactional
    public void unfollow(Long userId, String ticker) {
        watchlistRepository.deleteByUserIdAndTicker(userId, ticker.toUpperCase());
    }

    public List<String> getTickers(Long userId) {
        return watchlistRepository.findByUserIdOrderByTickerAsc(userId)
                .stream()
                .map(WatchlistItem::getTicker)
                .toList();
    }
}