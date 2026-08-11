import PositionItem from "./PositionItem";

export default function Positions({ items }) {
    if (items.length === 0) {
        return (
            <div className="empty-state">
                <h3>No positions synced yet</h3>
                <p>Sync your connected brokerage account to see your real holdings here.</p>
            </div>
        );
    }

    return (
        <div className="positions-grid">
            {items.map((item) => (
                <PositionItem
                    key={item.instrumentId}
                    instrumentId={item.instrumentId}
                    ticker={item.ticker}
                    name={item.name}
                    quantity={item.quantity}
                />
            ))}
        </div>
    );
}
