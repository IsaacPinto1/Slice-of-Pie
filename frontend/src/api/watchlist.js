import api from "./axios";

export function getWatchlist() {
    return api.get("/watchlist");
}

// query can be a ticker or company name - the backend resolves/creates
// the underlying Instrument for us.
export function addTicker(query) {
    return api.post(`/watchlist/${encodeURIComponent(query)}`);
}

export function removeTicker(instrumentId) {
    return api.delete(`/watchlist/${instrumentId}`);
}
