import { useEffect, useRef, useState } from "react";
import { getThesis } from "../api/thesis";
import { forceLatestPrice } from "../api/price";
import ThesisEditor from "./ThesisEditor";

// Minimum time between force-refresh clicks - mirrors WatchlistDetail's
// same cooldown/reasoning.
const FORCE_REFRESH_COOLDOWN_MS = 5000;

// Focused/large view for a single held position, shown in the detail
// panel once its sidebar card is selected. Unlike the old PositionItem,
// this never fetches a price on load - `item.price` already came from
// PositionItemResponse when the sidebar list loaded, so opening a
// position is a pure render, not a network call. "Force update price" is
// the one deliberate exception: it always hits the provider, and its
// result overrides the list price locally until the next sync or a
// different position is selected.
export default function PositionDetail({ item }) {
    const { instrumentId, ticker, name, quantity, costBasis } = item;
    const [thesis, setThesis] = useState("");
    const [loadingThesis, setLoadingThesis] = useState(true);
    const [forcedPrice, setForcedPrice] = useState(null);
    const [forcing, setForcing] = useState(false);
    const lastForceAtRef = useRef(0);

    // No need to reset forcedPrice/thesis on instrumentId change here -
    // DetailPanel keys this component by instrumentId, so switching
    // positions remounts it with fresh state entirely.
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

    const handleForceRefresh = async () => {
        const now = Date.now();
        if (forcing || now - lastForceAtRef.current < FORCE_REFRESH_COOLDOWN_MS) return;
        lastForceAtRef.current = now;

        setForcing(true);
        try {
            const res = await forceLatestPrice(instrumentId);
            setForcedPrice({
                price: res.data.price,
                priceUpdatedAt: res.data.priceUpdatedAt,
            });
        } catch {
            alert("Error getting price");
        } finally {
            setForcing(false);
        }
    };

    const price = forcedPrice?.price ?? item.price;
    const marketValue = price != null ? Number(quantity) * Number(price) : null;
    // Same "0 cost basis reads as unknown, not -100%" rule as
    // PortfolioSidebar's percentChange - a position that predates cost
    // basis tracking and hasn't been re-synced yet shouldn't show a
    // misleading number here either.
    const costBasisNum = Number(costBasis);
    const hasCostBasis = price != null && Number.isFinite(costBasisNum) && costBasisNum !== 0;
    const percentChange = hasCostBasis ? ((Number(price) - costBasisNum) / costBasisNum) * 100 : null;

    return (
        <div className="detail-card">
            <div className="watchlist-card-head">
                <div>
                    <div className="ticker-symbol detail-ticker">{ticker}</div>
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
                        {price != null ? `$${price}` : "—"}
                    </span>
                </div>
                <div className="position-metric">
                    <span className="position-metric-label">Value</span>
                    <span className="position-metric-value position-metric-value-accent">
                        {marketValue != null ? `$${marketValue.toFixed(2)}` : "—"}
                    </span>
                </div>
                <div className="position-metric">
                    <span className="position-metric-label">Cost basis</span>
                    <span className="position-metric-value">
                        {hasCostBasis ? `$${costBasisNum.toFixed(2)}` : "—"}
                    </span>
                </div>
                <div className="position-metric">
                    <span className="position-metric-label">Change</span>
                    <span
                        className={`position-metric-value${
                            percentChange != null ? (percentChange >= 0 ? " position-metric-positive" : " position-metric-negative") : ""
                        }`}
                    >
                        {percentChange != null ? `${percentChange >= 0 ? "+" : ""}${percentChange.toFixed(2)}%` : "—"}
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
