// Small "at a glance" card for a single row in the sidebar - used for
// both positions and watchlist items, since the two look identical at
// this size: just a ticker and its price, both served directly on
// PositionItemResponse/WatchlistItemResponse so rendering a whole
// sidebar of these never needs a per-item GET /price call.
//
// Deliberately minimal for now. Once cost basis is tracked on positions,
// this is the one place a "price · % change" stat (or any other future
// per-item stat) needs to be added for it to show up for every row -
// swap/extend the price span below rather than touching the sections or
// the detail views.
export default function SidebarItemCard({ item, active, onSelect }) {
    return (
        <button
            type="button"
            className={`sidebar-item${active ? " active" : ""}`}
            onClick={onSelect}
        >
            <span className="sidebar-item-ticker">{item.ticker}</span>
            <span className="sidebar-item-price">{formatPrice(item.price)}</span>
        </button>
    );
}

function formatPrice(price) {
    if (price == null) return "—";
    const num = Number(price);
    if (Number.isNaN(num)) return "—";
    return `$${num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}
