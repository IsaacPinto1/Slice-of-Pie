package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.broker.exception.BrokerAccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gate for the entire broker/positions feature. A SnapTrade Personal key
 * represents one real person's brokerage (see the spec's "Single-identity
 * caveat"), but this app's User model supports multiple accounts. Without
 * this guard, any signed-up app user could hit /positions/sync and pull
 * the key owner's real Robinhood holdings into their own account.
 *
 * Restricts the feature to a configured allowlist of app usernames, and
 * is invisible by design: callers throw BrokerAccessDeniedException,
 * which BrokerExceptionHandler maps to a plain 404 - not a 403 - so a
 * non-allowed authenticated user hitting these endpoints sees exactly
 * what they'd see for a route that doesn't exist.
 */
@Component
public class BrokerAccessGuard {

    private final Set<String> allowedUsernames;

    public BrokerAccessGuard(@Value("${snaptrade.allowed-usernames:}") String allowedUsernamesConfig) {
        this.allowedUsernames = Arrays.stream(allowedUsernamesConfig.split(","))
                .map(String::trim)
                .filter(username -> !username.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void assertAllowed(String username) {
        if (username == null || !allowedUsernames.contains(username)) {
            throw new BrokerAccessDeniedException();
        }
    }
}
