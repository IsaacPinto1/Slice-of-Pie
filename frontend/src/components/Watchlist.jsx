import WatchlistItem from "./WatchlistItem";

export default function Watchlist({ items, onRemove }) {
    if (items.length === 0) {
        return (
            <div className="empty-state">
                <h3>Your watchlist is empty</h3>
                <p>Add a ticker above to start tracking it and writing your thesis.</p>
            </div>
        );
    }

    return (
        <div className="watchlist-grid">
            {items.map((item) => (
                <WatchlistItem
                    key={item.instrumentId}
                    instrumentId={item.instrumentId}
                    ticker={item.ticker}
                    name={item.name}
                    onRemove={onRemove}
                />
            ))}
        </div>
    );
}
