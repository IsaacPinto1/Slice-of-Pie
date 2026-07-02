package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistItemResponse;
import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping("/{ticker}")
    public ResponseEntity<WatchlistItemResponse> follow(@PathVariable String ticker, Authentication authentication) {
        String ticker0 = watchlistService.follow(authentication.getName(), ticker);
        return ResponseEntity.ok(new WatchlistItemResponse(ticker0));
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<Void> unfollow(@PathVariable String ticker, Authentication authentication) {
        watchlistService.unfollow(authentication.getName(), ticker);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<WatchlistResponse> getWatchlist(Authentication authentication) {
        return ResponseEntity.ok(new WatchlistResponse(watchlistService.getTickers(authentication.getName())));
    }
}