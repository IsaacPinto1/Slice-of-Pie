import api from "./axios";

// Cheap allow-check - just an env-var lookup on the backend, no external
// broker call - meant to be fetched alongside /me and /watchlist so the
// frontend knows whether to render the Positions section before
// /broker/status (which can be slow) ever resolves. Always 200 with
// {allowed: true/false}, unlike the other broker routes below.
export function getBrokerAllowed() {
    return api.get("/broker/allowed");
}

// Not connected -> {connected: false}, not an error. A 404 here (user not
// on the backend allowlist) is treated the same way by Dashboard - see
// its comment on the initial load effect.
export function getBrokerStatus() {
    return api.get("/broker/status");
}
