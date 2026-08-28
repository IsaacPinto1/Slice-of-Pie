import PositionDetail from "./PositionDetail";
import WatchlistDetail from "./WatchlistDetail";

// Right-hand pane of the sidebar + focused-item layout. `selected` is
// null (nothing picked yet) or { type: "position" | "watchlist", item }
// with `item` already resolved to the current row - see Dashboard's
// `activeDetail` derivation for why that resolution happens there rather
// than here (keeps a removed/desynced row from ever reaching this panel).
//
// The `key` on each detail component forces a clean remount when the
// selected instrument changes, so per-item state (thesis, confirm-remove)
// never leaks from one ticker to the next. 
export default function DetailPanel({
    selected,
    onRemoveWatchlistItem,
    onPositionPriceUpdate,
    onWatchlistPriceUpdate,
}) {
    if (!selected) {
        return (
            <div className="detail-panel-empty empty-state">
                <h3>Nothing selected</h3>
                <p>Pick a position or watchlist ticker from the sidebar to see its details.</p>
            </div>
        );
    }

    if (selected.type === "position") {
        return (
            <PositionDetail
                key={selected.item.instrumentId}
                item={selected.item}
                onPriceUpdate={onPositionPriceUpdate}
            />
        );
    }

    return (
        <WatchlistDetail
            key={selected.item.instrumentId}
            item={selected.item}
            onRemove={onRemoveWatchlistItem}
            onPriceUpdate={onWatchlistPriceUpdate}
        />
    );
}
