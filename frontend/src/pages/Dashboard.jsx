import { useEffect, useState } from "react";
import Watchlist from "../components/Watchlist";
import Positions from "../components/Positions";
import ViewToggle from "../components/ViewToggle";
import TickerSearch from "../components/TickerSearch";
import { addTicker, getWatchlist, removeTicker } from "../api/watchlist";
import { createInstrument } from "../api/instruments";
import { getMe } from "../api/user";
import { getBrokerStatus } from "../api/broker";
import { getPositions, syncPositions } from "../api/positions";
import BrandMark from "../components/BrandMark";

export default function Dashboard() {
    const [username, setUsername] = useState("");
    const [watchlist, setWatchlist] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // Broker/positions state. brokerAllowed stays false until a real 200
    // comes back from GET /broker/status - a non-allowed user's request
    // fails (deliberately indistinguishable from a 404 on a route that
    // doesn't exist - see BrokerAccessGuard), so the safe default is
    // "assume not allowed" and only turn the feature on once it's actually
    // confirmed. That's what keeps this whole section - toggle included -
    // showing zero evidence positions exist for anyone not on the allowlist.
    const [brokerAllowed, setBrokerAllowed] = useState(false);
    const [connected, setConnected] = useState(false);
    const [positions, setPositions] = useState([]);
    const [positionsLoading, setPositionsLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [view, setView] = useState("both");

    // sync: true runs a fresh sync against the provider first (used on
    // initial load and the manual "Sync" button); sync: false just re-reads
    // whatever's already stored (used as a fallback if a sync attempt
    // itself fails, so a provider hiccup doesn't wipe the section).
    const loadPositions = async ({ sync }) => {
        setPositionsLoading(true);
        try {
            const res = sync ? await syncPositions() : await getPositions();
            setPositions(res.data.items);
        } catch {
            if (sync) {
                try {
                    const res = await getPositions();
                    setPositions(res.data.items);
                } catch {
                    setPositions([]);
                }
            }
        } finally {
            setPositionsLoading(false);
        }
    };

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

            // Broker/positions is a separate, best-effort load, deliberately
            // isolated from the try/catch above: a failure here - including
            // the 404 a non-allowed user gets - must never surface as a
            // dashboard error, block the watchlist from loading, or leave
            // any visible trace that this feature exists.
            try {
                const statusRes = await getBrokerStatus();
                if (cancelled) return;
                setBrokerAllowed(true);
                setConnected(statusRes.data.connected);

                if (statusRes.data.connected) {
                    await loadPositions({ sync: true });
                }
            } catch {
                if (!cancelled) setBrokerAllowed(false);
            }
        };

        load();
        return () => { cancelled = true; };
    }, []);

    const handleSyncPositions = async () => {
        if (syncing) return;
        setSyncing(true);
        try {
            await loadPositions({ sync: true });
        } finally {
            setSyncing(false);
        }
    };

    const reloadWatchlist = async () => {
        const res = await getWatchlist();
        setWatchlist(res.data.items);
    };

    // Runs when a result is picked from the search dropdown: creates the
    // Instrument (idempotent - a no-op if it already exists), then follows
    // it. This is the only way a ticker gets added now; TickerSearch
    // surfaces any failure itself, so we just let errors propagate to it.
    const handleSelectResult = async ({ ticker, name }) => {
        await createInstrument(ticker, name);
        await addTicker(ticker);
        await reloadWatchlist();
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
                            <h1>{brokerAllowed ? "Your portfolio" : "Your watchlist"}</h1>
                            {brokerAllowed && connected && (
                                <div className="dashboard-head-actions">
                                    <ViewToggle value={view} onChange={setView} />
                                    <button
                                        className="secondary small"
                                        onClick={handleSyncPositions}
                                        disabled={syncing}
                                    >
                                        {syncing ? "Syncing..." : "Sync positions"}
                                    </button>
                                </div>
                            )}
                        </div>

                        {error && <div className="banner error">{error}</div>}

                        {(!brokerAllowed || view !== "positions") && (
                            <div className="ticker-search">
                                <label htmlFor="ticker-search-input">Add to watchlist</label>
                                <TickerSearch onSelect={handleSelectResult} />
                            </div>
                        )}

                        {brokerAllowed && connected && (view === "positions" || view === "both") && (
                            <section className="dashboard-section">
                                {view === "both" && <h2 className="dashboard-section-title">Positions</h2>}
                                {positionsLoading ? (
                                    <div className="loading-row">
                                        <span className="spinner" />
                                        <span>Loading positions...</span>
                                    </div>
                                ) : (
                                    <Positions items={positions} />
                                )}
                            </section>
                        )}

                        {(!brokerAllowed || !connected || view === "watchlist" || view === "both") && (
                            <section className="dashboard-section">
                                {brokerAllowed && connected && view === "both" && (
                                    <h2 className="dashboard-section-title">Watchlist</h2>
                                )}
                                <Watchlist items={watchlist} onRemove={handleRemove} />
                            </section>
                        )}
                    </>
                )}
            </div>
        </>
    );
}
