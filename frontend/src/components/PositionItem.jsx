import { useEffect, useRef, useState } from "react";
import { getThesis } from "../api/thesis";
import { getPrice, forceLatestPrice } from "../api/price";
import ThesisEditor from "./ThesisEditor";

// Minimum time between force-refresh clicks - mirrors WatchlistItem's
// same cooldown/reasoning.
const FORCE_REFRESH_COOLDOWN_MS = 5000;

// Mirrors WatchlistItem's price-fetching and thesis behavior almost
// exactly. The meaningful differences are structural, not just visual, so
// this stays a separate component rather than a branch inside
// WatchlistItem:
//   - no remove action - a Position only ever changes via a provider sync
//     (PositionService#sync's full reconciliation), never a manual delete
//   - a quantity, sourced from the actual brokerage holding
//   - a computed market value (quantity * price), which is the whole
//     reason a position needs its own layout instead of reusing
//     WatchlistItem's price-row as-is
export default function PositionItem({ instrumentId, ticker, name, quantity }) {
    const [thesis, setThesis] = useState("");
    const [loadingThesis, setLoadingThesis] = useState(true);
    const [price, setPrice] = useState(null);
    const [priceLoading, setPriceLoading] = useState(true);
    const [forcing, setForcing] = useState(false);
    const lastForceAtRef = useRef(0);

    useEffect(() => {
        let cancelled = false;

        const loadThesis = async () => {
            setLoadingThesis(true);
            try {
                const res = await getThesis(instrumentId);
                // A thesis that hasn't been written yet comes back as 204 No Content.
                if (!cancelled) setThesis(res.data?.content ?? "");
            } catch {
                if (!cancelled) setThesis("");
            } finally {
                if (!cancelled) setLoadingThesis(false);
            }
        };

        loadThesis();
        return () => { cancelled = true; };
    }, [instrumentId]);

    useEffect(() => {
        let cancelled = false;

        const loadPrice = async () => {
            setPriceLoading(true);
            try {
                const res = await getPrice(instrumentId);
                if (!cancelled) setPrice(res.data.price);
            } catch {
                if (!cancelled) setPrice(null);
            } finally {
                if (!cancelled) setPriceLoading(false);
            }
        };

        loadPrice();
        return () => { cancelled = true; };
    }, [instrumentId]);

    const handleForceRefresh = async () => {
        const now = Date.now();
        if (forcing || now - lastForceAtRef.current < FORCE_REFRESH_COOLDOWN_MS) return;
        lastForceAtRef.current = now;

        setForcing(true);
        try {
            const res = await forceLatestPrice(instrumentId);
            setPrice(res.data.price);
        } catch {
            alert("Error getting price");
        } finally {
            setForcing(false);
        }
    };

    const marketValue = price != null ? Number(quantity) * Number(price) : null;

    return (
        <div className="position-card">
            <div className="watchlist-card-head">
                <div>
                    <div className="ticker-symbol">{ticker}</div>
                    {name && <div className="ticker-name">{name}</div>}
                </div>
                <span className="position-badge">Held</span>
            </div>

            <div className="position-metrics">
                <div className="position-metric">
                    <span className="position-metric-label">Quantity</span>
                    <span className="position-metric-value">{formatQuantity(quantity)}</span>
                </div>
                <div className="position-metric">
                    <span className="position-metric-label">Price</span>
                    <span className="position-metric-value">
                        {priceLoading ? <span className="spinner" /> : price != null ? `$${price}` : "—"}
                    </span>
                </div>
                <div className="position-metric">
                    <span className="position-metric-label">Value</span>
                    <span className="position-metric-value position-metric-value-accent">
                        {priceLoading ? (
                            <span className="spinner" />
                        ) : marketValue != null ? (
                            `$${marketValue.toFixed(2)}`
                        ) : (
                            "—"
                        )}
                    </span>
                </div>
            </div>

            <div className="price-row">
                <button
                    type="button"
                    className="icon-button"
                    onClick={handleForceRefresh}
                    disabled={forcing}
                    title="Force update price"
                    aria-label="Force update price"
                >
                    <svg
                        className={forcing ? "spin" : ""}
                        viewBox="0 0 24 24"
                        width="14"
                        height="14"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2.2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <polyline points="23 4 23 10 17 10" />
                        <polyline points="1 20 1 14 7 14" />
                        <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
                    </svg>
                </button>
                <span className="position-refresh-label">Refresh price</span>
            </div>

            {loadingThesis ? (
                <div className="loading-row" style={{ padding: "8px 0" }}>
                    <span className="spinner" />
                    <span>Loading thesis...</span>
                </div>
            ) : (
                <ThesisEditor
                    instrumentId={instrumentId}
                    thesis={thesis}
                    setThesis={setThesis}
                />
            )}
        </div>
    );
}

function formatQuantity(quantity) {
    const num = Number(quantity);
    if (Number.isNaN(num)) return quantity;
    // Fractional shares are common (Robinhood etc.) - keep up to 4 decimal
    // places but trim trailing zeros so whole-share holdings look clean.
    return num.toLocaleString(undefined, { maximumFractionDigits: 4 });
}
