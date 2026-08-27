// Shared staleness check for anything carrying priceUpdatedAt/staleAfterMinutes
// (PriceResponse, WatchlistItemResponse, PositionItemResponse - see the
// backend DTOs). Centralized so SidebarItemCard, PositionDetail, and
// WatchlistDetail all agree on what "stale" means instead of each
// re-deriving it.
export function isPriceStale(item) {
    if (!item || item.priceUpdatedAt == null) return false;

    const updatedAt = new Date(item.priceUpdatedAt).getTime();
    if (Number.isNaN(updatedAt)) return false;

    // staleAfterMinutes rides along on every response so the frontend never
    // has to hardcode/duplicate the backend's PriceService.STALE_AFTER_MINUTES.
    const staleAfterMinutes = item.staleAfterMinutes ?? 60;
    const ageMs = Date.now() - updatedAt;

    return ageMs > staleAfterMinutes * 60 * 1000;
}
