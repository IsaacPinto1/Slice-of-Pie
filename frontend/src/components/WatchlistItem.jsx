import { useEffect, useState } from "react";
import { getThesis } from "../api/thesis";
import { getPrice } from "../api/price";
import ThesisEditor from "./ThesisEditor";

export default function WatchlistItem({ instrumentId, ticker, name, onRemove }) {
    const [thesis, setThesis] = useState("");
    const [loadingThesis, setLoadingThesis] = useState(true);
    const [removing, setRemoving] = useState(false);
    const [confirmingRemove, setConfirmingRemove] = useState(false);
    const [price, setPrice] = useState(0);

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

    const handlePrice = async () => {
        try{
            const res = await getPrice(ticker);
            setPrice(res.data.price);
        } catch {
            alert("Error getting price")
        }
    }

    return (
        <div className="watchlist-card">
            <div className="watchlist-card-head">
                <div>
                    <div className="ticker-symbol">{ticker}</div>
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

            <div>{price}</div>

            <button
            onClick={()=>handlePrice()}>
                refresh Price
            </button>

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
