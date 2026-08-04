import { useEffect, useState } from "react";
import Watchlist from "../components/Watchlist";
import { addTicker, getWatchlist, removeTicker } from "../api/watchlist";
import { getMe } from "../api/user";
import BrandMark from "../components/BrandMark";

export default function Dashboard() {
    const [username, setUsername] = useState("");
    const [watchlist, setWatchlist] = useState([]);
    const [newTicker, setNewTicker] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [adding, setAdding] = useState(false);
    const [addError, setAddError] = useState("");

    useEffect(() => {
        let cancelled = false;

        const load = async () => {
            setLoading(true);
            setError("");
            try {
                const [meRes, watchlistRes] = await Promise.all([
                    getMe(),
                    getWatchlist(),
                ]);
                if (cancelled) return;
                setUsername(meRes.data.username);
                setWatchlist(watchlistRes.data.items);
            } catch {
                if (!cancelled) setError("Couldn't load your dashboard. Try refreshing.");
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        load();
        return () => { cancelled = true; };
    }, []);

    const reloadWatchlist = async () => {
        const res = await getWatchlist();
        setWatchlist(res.data.items);
    };

    const handleAdd = async (e) => {
        e.preventDefault();
        const query = newTicker.trim();
        if (!query || adding) return;

        setAdding(true);
        setAddError("");
        try {
            await addTicker(query);
            setNewTicker("");
            await reloadWatchlist();
        } catch (err) {
            setAddError(
                err.response?.status === 404
                    ? `Couldn't find a ticker matching "${query}".`
                    : "Couldn't add that to your watchlist. Try again."
            );
        } finally {
            setAdding(false);
        }
    };

    const handleRemove = async (instrumentId) => {
        const previous = watchlist;
        // Optimistic update - the list feels instant, and we roll back on failure.
        setWatchlist(watchlist.filter((item) => item.instrumentId !== instrumentId));
        try {
            await removeTicker(instrumentId);
        } catch {
            setWatchlist(previous);
            setError("Couldn't remove that ticker. Try again.");
        }
    };

    const logout = () => {
        localStorage.removeItem("token");
        window.location.href = "/login";
    };

    return (
        <>
            <header className="app-header">
                <div className="brand">
                    <BrandMark />
                    <span className="brand-name">Slice of Pie</span>
                </div>
                <div className="header-right">
                    {username && <span className="header-user">{username}</span>}
                    <button className="secondary small" onClick={logout}>
                        Log out
                    </button>
                </div>
            </header>

            <div className="page">
                {loading ? (
                    <div className="loading-row">
                        <span className="spinner" />
                        <span>Loading your dashboard...</span>
                    </div>
                ) : (
                    <>
                        <div className="dashboard-head">
                            <h1>Your watchlist</h1>
                        </div>

                        {error && <div className="banner error">{error}</div>}

                        <div className="ticker-search">
                            <label htmlFor="ticker-search-input">Add to watchlist</label>
                            <form className="add-ticker-form" onSubmit={handleAdd}>
                                <div className="ticker-search-input-wrap">
                                    <input
                                        id="ticker-search-input"
                                        type="text"
                                        aria-label="Add a ticker or company name"
                                        placeholder="Ticker or company name (e.g. AAPL)"
                                        value={newTicker}
                                        onChange={(e) => setNewTicker(e.target.value.toUpperCase())}
                                    />
                                </div>
                                <button type="submit" disabled={adding || !newTicker.trim()}>
                                    {adding ? "Adding..." : "Add"}
                                </button>
                            </form>
                            {addError && <p className="field-error">{addError}</p>}
                        </div>

                        <Watchlist items={watchlist} onRemove={handleRemove} />
                    </>
                )}
            </div>
        </>
    );
}
