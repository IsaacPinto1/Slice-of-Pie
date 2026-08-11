package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.broker.exception.BrokerAccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ensures that on deployment only a secret list of usernames can access the brokerage
 * through the broker-specific env vars. Importantly, usernames should be taken out of
 * a token on call, not passed as a String (requires users to know the username and password)
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
