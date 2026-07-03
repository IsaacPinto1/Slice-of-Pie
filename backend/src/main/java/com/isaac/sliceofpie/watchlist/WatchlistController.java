package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistItemResponse;
import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistResponse;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping("/{query}")
    public WatchlistItemResponse follow(@PathVariable String query, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        WatchlistItem item = watchlistService.follow(principal.id(), query);
        return WatchlistItemResponse.from(item);
    }

    @DeleteMapping("/{instrumentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long instrumentId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        watchlistService.unfollow(principal.id(), instrumentId);
    }

    @GetMapping
    public WatchlistResponse getWatchlist(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.id();
        List<Long> instrumentIds = watchlistService.listForUser(userId).stream()
                .map(item -> item.getInstrument().getId())
                .toList();
        return new WatchlistResponse(instrumentIds);
    }
}