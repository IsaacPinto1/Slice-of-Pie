import api from "./axios";

// Read-only search-as-you-type lookup - never creates anything.
// Returns up to the backend's configured cap of candidates
// (InstrumentResolutionService.MAX_SEARCH_RESULTS): [{ ticker, name }, ...]
export function searchInstruments(query) {
    return api.get("/instruments/search", { params: { q: query } });
}

// The only place an Instrument gets created - call this with a result the
// user picked out of the search dropdown (ticker + name both come from that
// result, not free text).
export function createInstrument(ticker, name) {
    return api.post("/instruments", { ticker, name });
}
