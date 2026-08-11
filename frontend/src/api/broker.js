import api from "./axios";

// Not connected -> {connected: false}, not an error. A 404 here (user not
// on the backend allowlist) is treated the same way by Dashboard - see
// its comment on the initial load effect.
export function getBrokerStatus() {
    return api.get("/broker/status");
}
