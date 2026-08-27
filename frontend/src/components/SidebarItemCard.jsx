import { isPriceStale } from "../utils/price";

// Small "at a glance" card for a single row in the sidebar - used for
// both positions and watchlist items, since the two look identical at
// this size: just a ticker and its price, both served directly on
// PositionItemResponse/WatchlistItemResponse so rendering a whole
// sidebar of these never needs a per-item GET /price call.
//
// `percentChange` is only ever set on position items (PortfolioSidebar
// derives it client-side from price vs. costBasis before this renders) -
// watchlist items simply don't have it, so the badge only shows up for
// held positions.
export default function SidebarItemCard({ item, active, onSelect }) {
    const stale = isPriceStale(item);
    return (
        <button
            type="button"
            className={`sidebar-item${active ? " active" : ""}`}
            onClick={onSelect}
        >
            <span className="sidebar-item-ticker">{item.ticker}</span>
            <span className="sidebar-item-stats">
                <span
                    className={`sidebar-item-price${stale ? " price-stale" : ""}`}
                    title={stale ? "Price may be out of date" : undefined}
                >
                    {formatPrice(item.price)}
                </span>
                {item.percentChange != null && (
                    <span className={`sidebar-item-change ${item.percentChange >= 0 ? "positive" : "negative"}`}>
                        {formatPercentChange(item.percentChange)}
                    </span>
                )}
            </span>
        </button>
    );
}

function formatPrice(price) {
    if (price == null) return "—";
    const num = Number(price);
    if (Number.isNaN(num)) return "—";
    return `$${num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatPercentChange(percentChange) {
    const sign = percentChange >= 0 ? "+" : "";
    return `${sign}${percentChange.toFixed(2)}%`;
}
