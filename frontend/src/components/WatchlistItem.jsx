import { useEffect, useState } from "react";
import { getThesis } from "../api/thesis";
import ThesisEditor from "./ThesisEditor";

export default function WatchlistItem({ instrumentId, ticker, name, onRemove }) {
    const [thesis, setThesis] = useState("");
    const [loadingThesis, setLoadingThesis] = useState(true);
    const [removing, setRemoving] = useState(false);

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
        }
    };

    return (
        <div className="watchlist-card">
            <div className="watchlist-card-head">
                <div>
                    <div className="ticker-symbol">{ticker}</div>
                    {name && <div className="ticker-name">{name}</div>}
                </div>
                <button
                    className="danger small"
                    onClick={handleRemove}
                    disabled={removing}
                >
                    {removing ? "Removing..." : "Remove"}
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
