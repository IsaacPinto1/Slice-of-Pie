import { useEffect, useRef, useState } from "react";
import { getThesis } from "../api/thesis";
import { forceLatestPrice } from "../api/price";
import { isPriceStale } from "../utils/price";
import ThesisEditor from "./ThesisEditor";

// Minimum time between force-refresh clicks. Every click is a guaranteed
// provider call (no cache), so this is what actually protects the API from
// being hammered by an impatient click - disabling the button while a
// request is in flight isn't enough on its own since requests usually
// resolve in well under a second.
const FORCE_REFRESH_COOLDOWN_MS = 5000;

// Focused/large view for a single watchlist ticker, shown in the detail
// panel once its sidebar card is selected. Like PositionDetail, this
// never fetches a price on load - `item.price` already came from
// WatchlistItemResponse when the sidebar list loaded.
export default function WatchlistDetail({ item, onRemove }) {
    const { instrumentId, ticker, name } = item;
    const [thesis, setThesis] = useState("");
    const [loadingThesis, setLoadingThesis] = useState(true);
    const [removing, setRemoving] = useState(false);
    const [confirmingRemove, setConfirmingRemove] = useState(false);
    const [forcedPrice, setForcedPrice] = useState(null);
    const [forcing, setForcing] = useState(false);
    const lastForceAtRef = useRef(0);

    // No need to reset forcedPrice/confirmingRemove/thesis on instrumentId
    // change here - DetailPanel keys this component by instrumentId, so
    // switching tickers remounts it with fresh state entirely.
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

    const handleRemove = async () => {
        setRemoving(true);
        try {
            await onRemove(instrumentId);
        } finally {
            setRemoving(false);
            setConfirmingRemove(false);
        }
    };

    const handleForceRefresh = async () => {
        const now = Date.now();
        if (forcing || now - lastForceAtRef.current < FORCE_REFRESH_COOLDOWN_MS) return;
        lastForceAtRef.current = now;

        setForcing(true);
        try {
            const res = await forceLatestPrice(instrumentId);
            // Widened to carry the forced response's priceUpdatedAt (and
            // staleAfterMinutes), not just the raw number - see
            // PositionDetail's identical change for why.
            setForcedPrice({
                price: res.data.price,
                priceUpdatedAt: res.data.priceUpdatedAt,
                staleAfterMinutes: res.data.staleAfterMinutes,
            });
        } catch {
            alert("Error getting price");
        } finally {
            setForcing(false);
        }
    };

    const price = forcedPrice?.price ?? item.price;
    const stale = isPriceStale(forcedPrice ?? item);

    return (
        <div className="detail-card">
            <div className="watchlist-card-head">
                <div>
                    <div className="ticker-symbol detail-ticker">{ticker}</div>
                    {name && <div className="ticker-name">{name}</div>}
                </div>
                {confirmingRemove ? (
                    <div className="confirm-remove">
                        <span className="confirm-remove-label">Remove {ticker}?</span>
                        <button
                            className="danger small"
                            onClick={handleRemove}
                            disabled={removing}
                        >
                            {removing ? "Removing..." : "Yes, remove"}
                        </button>
                        <button
                            className="secondary small"
                            onClick={() => setConfirmingRemove(false)}
                            disabled={removing}
                        >
                            Cancel
                        </button>
                    </div>
                ) : (
                    <button
                        className="danger small"
                        onClick={() => setConfirmingRemove(true)}
                    >
                        Remove
                    </button>
                )}
            </div>

            <div className="price-row">
                <span
                    className={`price-value${stale ? " price-stale" : ""}`}
                    title={stale ? "Price may be out of date" : undefined}
                >
                    {price != null ? `$${price}` : "—"}
                    {stale && <span className="stale-dot" aria-label="stale price" />}
                </span>
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
