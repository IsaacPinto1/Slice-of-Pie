package com.isaac.sliceofpie.broker.exception;

/**
 * Thrown by BrokerAccessGuard when an authenticated app user isn't on the
 * broker-feature allowlist. Mapped to a plain 404, not a 403 - per product
 * decision this feature must be invisible to everyone but the allowed
 * usernames, not just refused. A 403 would confirm to any logged-in user
 * that a broker/positions feature exists at all; a 404 looks identical to
 * hitting a route that was never defined.
 */
public class BrokerAccessDeniedException extends RuntimeException {
}
