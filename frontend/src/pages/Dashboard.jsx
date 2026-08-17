import { useEffect, useRef, useState } from "react";
import PortfolioSidebar from "../components/PortfolioSidebar";
import DetailPanel from "../components/DetailPanel";
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
    // confirmed. That's what keeps the whole Positions sidebar section -
    // and everything in it - showing zero evidence positions exist for
    // anyone not on the allowlist.
    const [brokerAllowed, setBrokerAllowed] = useState(false);
    const [connected, setConnected] = useState(false);
    const [positions, setPositions] = useState([]);
    // positionsLoading: true only while there's genuinely nothing to show
    // yet (the very first DB read on mount) - this is what lets
    // PortfolioSidebar fall back to its spinner-only state instead of
    // rendering an empty list.
    const [positionsLoading, setPositionsLoading] = useState(false);
    // positionsSyncing: a live reconciliation against the broker is in
    // flight (either the automatic background one on load, or the manual
    // "Sync positions" button). Deliberately separate from
    // positionsLoading - whatever's already on screen (freshly read from
    // the DB, or left over from the previous sync) stays exactly as-is
    // the whole time; this only drives a small indicator, never a
    // full-list spinner.
    const [positionsSyncing, setPositionsSyncing] = useState(false);

    // Guards state updates from in-flight requests (the background sync
    // in particular) that resolve after the component's gone - same
    // "cancelled" idea the load effect below uses locally, but shared
    // since syncPositionsInBackground can outlive that effect.
    //
    // The setup body must explicitly set this true, not just rely on
    // useRef(true)'s initial value: StrictMode double-invokes every
    // effect on mount (mount -> cleanup -> mount again) to catch exactly
    // this kind of bug, and that simulated cleanup flips the ref false.
    // Without resetting it here, the ref would stay false for the rest
    // of the component's real life, silently dropping every future
    // "if (mountedRef.current) setState(...)" - which is exactly what
    // made positionsSyncing get stuck true forever.
    const mountedRef = useRef(true);
    useEffect(() => {
        mountedRef.current = true;
        return () => { mountedRef.current = false; };
    }, []);

    // Which sidebar row is open in the detail panel. Deliberately just a
    // {type, instrumentId} pointer rather than a copy of the row itself -
    // the actual item is resolved fresh from positions/watchlist below on
    // every render (see `activeDetail`), so a sync, an add, or a remove is
    // reflected in whatever's open without a separate "refresh the
    // selection" effect, and a removed row just makes the panel empty again.
    const [selected, setSelected] = useState(null);
    const [positionsCollapsed, setPositionsCollapsed] = useState(false);
    const [watchlistCollapsed, setWatchlistCollapsed] = useState(false);

    // Cheap DB read - never touches the live broker, so this is always
    // fast. Used to hydrate the Positions section immediately on load
    // (the "worst case" fallback the background sync doesn't need to
    // wait on).
    const loadStoredPositions = async () => {
        setPositionsLoading(true);
        try {
            const res = await getPositions();
            setPositions(res.data.items);
        } catch {
            setPositions([]);
        } finally {
            setPositionsLoading(false);
        }
    };

    // Reconciles with the live provider. Deliberately never touches
    // positionsLoading - whatever's already rendered stays on screen for
    // the whole call; positionsSyncing only drives a small indicator in
    // the section header (see PortfolioSidebar), not a full-list
    // spinner. A failure here is swallowed rather than surfaced or
    // re-fetched: the DB snapshot already showing is a perfectly good
    // fallback, and a background reconciliation erroring out isn't
    // something the user needs to react to.
    const syncPositionsInBackground = async () => {
        if (positionsSyncing) return;
        setPositionsSyncing(true);
        try {
            const res = await syncPositions();
            if (mountedRef.current) setPositions(res.data.items);
        } catch {
            // keep whatever's already shown
        } finally {
            if (mountedRef.current) setPositionsSyncing(false);
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
                    // Show whatever's already stored immediately - a fast
                    // DB read, never blocked on SnapTrade - then
                    // reconcile with the provider quietly afterward. The
                    // list is never blanked in favor of a spinner here.
                    await loadStoredPositions();
                    if (cancelled) return;
                    syncPositionsInBackground();
                }
            } catch {
                if (!cancelled) setBrokerAllowed(false);
            }
        };

        load();
        return () => { cancelled = true; };
    }, []);

    const handleSyncPositions = () => {
        syncPositionsInBackground();
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

    // Resolve the selection pointer against the live lists on every render
    // - see the `selected` state comment above.
    const selectedRow = selected
        ? (selected.type === "position"
            ? positions.find((p) => p.instrumentId === selected.instrumentId)
            : watchlist.find((w) => w.instrumentId === selected.instrumentId))
        : null;
    const activeDetail = selectedRow ? { type: selected.type, item: selectedRow } : null;

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

            <div className="page page-wide">
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
                                    <button
                                        className="secondary small"
                                        onClick={handleSyncPositions}
                                        disabled={positionsSyncing}
                                    >
                                        {positionsSyncing ? "Syncing..." : "Sync positions"}
                                    </button>
                                </div>
                            )}
                        </div>

                        {error && <div className="banner error">{error}</div>}

                        <div className="ticker-search">
                            <label htmlFor="ticker-search-input">Add to watchlist</label>
                            <TickerSearch onSelect={handleSelectResult} />
                        </div>

                        <div className="dashboard-layout">
                            <PortfolioSidebar
                                brokerAllowed={brokerAllowed}
                                connected={connected}
                                positions={positions}
                                positionsLoading={positionsLoading}
                                positionsSyncing={positionsSyncing}
                                watchlist={watchlist}
                                selected={selected}
                                onSelect={setSelected}
                                positionsCollapsed={positionsCollapsed}
                                onTogglePositions={() => setPositionsCollapsed((c) => !c)}
                                watchlistCollapsed={watchlistCollapsed}
                                onToggleWatchlist={() => setWatchlistCollapsed((c) => !c)}
                            />

                            <main className="detail-panel">
                                <DetailPanel selected={activeDetail} onRemoveWatchlistItem={handleRemove} />
                            </main>
                        </div>
                    </>
                )}
            </div>
        </>
    );
}
