import api from "./axios";

export function getPositions() {
    return api.get("/positions");
}

// Full reconciliation on the backend - replaces local state with the
// response instead of re-fetching.
export function syncPositions() {
    return api.post("/positions/sync");
}
