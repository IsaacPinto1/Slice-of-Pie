package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
import com.isaac.sliceofpie.broker.PositionDtos.BrokerStatusResponse;
import com.isaac.sliceofpie.broker.PositionDtos.PositionItemResponse;
import com.isaac.sliceofpie.broker.PositionDtos.PositionResponse;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single controller for the whole broker/positions feature: connection
 * status, reading synced positions, and triggering a sync.
 * 
 * For now this supports a single user per-app, supported via env vars.
 * In future there would need to be a register user flow with an appropriate
 * provider. Current implementation makes sense for a personal use case
 *
 */
@RestController
public class PositionController {

    private final PositionService positionService;
    private final BrokerAccessGuard brokerAccessGuard;

    public PositionController(PositionService positionService, BrokerAccessGuard brokerAccessGuard) {
        this.positionService = positionService;
        this.brokerAccessGuard = brokerAccessGuard;
    }

    @GetMapping("/broker/status")
    public BrokerStatusResponse status(Authentication authentication) {
        allow(authentication);
        return new BrokerStatusResponse(positionService.hasConnections());
    }

    /* Makes an external call to pull and store positions. Should be run before
    * attempting a GET so that positions are loaded. Returns the same shape as the GET
    * method so this can be used on the frontend to replace existing state.
     */
    @PostMapping("/positions/sync")
    public PositionResponse sync(Authentication authentication) {
        UserPrincipal principal = allow(authentication);
        return toResponse(positionService.sync(principal.id()));
    }

    // Mirrors WatchlistController#getWatchlist, plus the allowlist gate -
    // see BrokerAccessGuard. Note that this just pulls from the database,
    // sync is required first to load positions
    @GetMapping("/positions")
    public PositionResponse getPositions(Authentication authentication) {
        UserPrincipal principal = allow(authentication);
        return toResponse(positionService.listForUser(principal.id()));
    }


    private UserPrincipal allow(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        brokerAccessGuard.assertAllowed(principal.username());
        return principal;
    }

    private PositionResponse toResponse(List<Position> positions) {
        List<PositionItemResponse> items = positions.stream()
                .map(PositionItemResponse::from)
                .toList();
        return new PositionResponse(items);
    }
}
