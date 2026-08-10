import api from './axios'

// Cached read: returns the stored price if it's still fresh, otherwise
// looks up + persists a new one. Safe to call on every load - this is
// what keeps prices warm for whatever's actually being viewed, alongside
// the backend's scheduled background refresh for everything else.
export function getPrice(instrumentId){
    return api.get('/price', {params: {instrumentId: instrumentId}} )
}

// Bypasses the cache and always looks up a fresh price. Reserved for the
// explicit "force update" action - debounced client-side since every call
// here is a guaranteed provider hit.
export function forceLatestPrice(instrumentId){
    return api.get('/price/force', {params: {instrumentId: instrumentId}} )
}