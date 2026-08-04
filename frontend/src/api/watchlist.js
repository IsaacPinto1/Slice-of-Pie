import api from "./axios";

export function getWatchlist() {
    return api.get("/watchlist");
}

// ticker must already exist as an Instrument (created via the search ->
// select -> create flow in TickerSearch) - the backend no longer creates
// one on the fly here.
export function addTicker(ticker) {
    return api.post(`/watchlist/${encodeURIComponent(ticker)}`);
}

export function removeTicker(instrumentId) {
    return api.delete(`/watchlist/${instrumentId}`);
}
