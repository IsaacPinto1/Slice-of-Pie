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
 * NOTE (Single-identity caveat, see spec): a brokerage Personal key
 * represents one real person's brokerage. Any app user who passes
 * BrokerAccessGuard sees the SAME underlying provider identity - there is
 * no way to distinguish "app user A's brokerage" from "app user B's" at
 * the provider layer. BrokerAccessGuard's allowlist is what keeps this
 * from being a real data-exposure bug if this app ever gets a second real
 * user; don't remove it without re-reading the spec's caveat section.
 *
 * No POST /broker/connect and no connection-portal callback route - this
 * Personal key is linked to Robinhood out-of-band (via the provider's own
 * dashboard), not through an in-app registration flow. This controller
 * only reports whether that out-of-band link currently has connected
 * accounts, and lets the allowed user pull/sync positions from it.
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

    // Mirrors WatchlistController#getWatchlist, plus the allowlist gate -
    // see BrokerAccessGuard.
    @GetMapping("/positions")
    public PositionResponse getPositions(Authentication authentication) {
        UserPrincipal principal = allow(authentication);
        return toResponse(positionService.listForUser(principal.id()));
    }

    // Returns the same response shape as GET, so the frontend can just
    // replace its state with this response instead of re-fetching.
    @PostMapping("/positions/sync")
    public PositionResponse sync(Authentication authentication) {
        UserPrincipal principal = allow(authentication);
        return toResponse(positionService.sync(principal.id()));
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
