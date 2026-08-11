package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
import com.isaac.sliceofpie.broker.BrokerDtos.BrokerStatusResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NOTE (Single-identity caveat, see spec): a SnapTrade Personal key
 * represents one real person's brokerage. Any app user who passes
 * BrokerAccessGuard sees the SAME underlying SnapTrade identity - there is
 * no way to distinguish "app user A's brokerage" from "app user B's" at
 * the SnapTrade layer. BrokerAccessGuard's allowlist is what keeps this
 * from being a real data-exposure bug if this app ever gets a second real
 * user; don't remove it without re-reading the spec's caveat section.
 *
 * No POST /broker/snaptrade/connect and no connection-portal callback
 * route - this Personal key is linked to Robinhood out-of-band (via
 * SnapTrade's own dashboard), not through an in-app registration flow.
 * This controller only reports whether that out-of-band link currently
 * has connected accounts.
 */
@RestController
@RequestMapping("/broker")
public class BrokerController {

    private final BrokerConnectionService brokerConnectionService;
    private final BrokerAccessGuard brokerAccessGuard;

    public BrokerController(BrokerConnectionService brokerConnectionService,
                             BrokerAccessGuard brokerAccessGuard) {
        this.brokerConnectionService = brokerConnectionService;
        this.brokerAccessGuard = brokerAccessGuard;
    }

    @GetMapping("/snaptrade/status")
    public BrokerStatusResponse status(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        brokerAccessGuard.assertAllowed(principal.username());

        return new BrokerStatusResponse(brokerConnectionService.isConnected());
    }
}
