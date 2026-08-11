package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
import com.isaac.sliceofpie.broker.PositionDtos.PositionItemResponse;
import com.isaac.sliceofpie.broker.PositionDtos.PositionResponse;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/positions")
public class PositionController {

    private final PositionSyncService positionSyncService;
    private final PositionRepository positionRepository;
    private final BrokerAccessGuard brokerAccessGuard;

    public PositionController(PositionSyncService positionSyncService,
                               PositionRepository positionRepository,
                               BrokerAccessGuard brokerAccessGuard) {
        this.positionSyncService = positionSyncService;
        this.positionRepository = positionRepository;
        this.brokerAccessGuard = brokerAccessGuard;
    }

    // Mirrors WatchlistController#getWatchlist exactly, plus the allowlist
    // gate - see BrokerAccessGuard.
    @GetMapping
    public PositionResponse getPositions(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        brokerAccessGuard.assertAllowed(principal.username());

        List<PositionItemResponse> items = positionRepository.findAllByUserIdFetchInstrument(principal.id())
                .stream()
                .map(PositionItemResponse::from)
                .toList();
        return new PositionResponse(items);
    }

    // Returns the same response shape as GET, so the frontend can just
    // replace its state with this response instead of re-fetching.
    @PostMapping("/sync")
    public PositionResponse sync(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        brokerAccessGuard.assertAllowed(principal.username());

        List<PositionItemResponse> items = positionSyncService.sync(principal.id()).stream()
                .map(PositionItemResponse::from)
                .toList();
        return new PositionResponse(items);
    }
}
