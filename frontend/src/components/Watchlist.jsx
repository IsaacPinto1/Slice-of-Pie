import WatchlistItem from "./WatchlistItem";

export default function Watchlist({ tickers, onRemove }) {
    return (
        <div>
            <h3>Your Watchlist</h3>

            {tickers.map((ticker) => (
                <WatchlistItem
                    key={ticker}
                    ticker={ticker}
                    onRemove={onRemove}
                />
            ))}
        </div>
    );
}