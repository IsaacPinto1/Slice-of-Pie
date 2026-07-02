import api from "./axios";

export function getWatchlist() {
    return api.get("/watchlist");
}

export function addTicker(ticker) {
    return api.post(`/watchlist/${ticker}`);
}

export function removeTicker(ticker) {
    return api.delete(`/watchlist/${ticker}`);
}