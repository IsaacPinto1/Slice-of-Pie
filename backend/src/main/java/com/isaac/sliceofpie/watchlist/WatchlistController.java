package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
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
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String result = watchlistService.follow(principal.id(), ticker);
        return ResponseEntity.ok(new WatchlistItemResponse(result));
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<Void> unfollow(@PathVariable String ticker, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        watchlistService.unfollow(principal.id(), ticker);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<WatchlistResponse> getWatchlist(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new WatchlistResponse(watchlistService.getTickers(principal.id())));
    }
}