import { useEffect, useRef, useState } from "react";
import PortfolioSidebar from "../components/PortfolioSidebar";
import DetailPanel from "../components/DetailPanel";
import TickerSearch from "../components/TickerSearch";
import { addTicker, getWatchlist, removeTicker } from "../api/watchlist";
import { createInstrument } from "../api/instruments";
import { getMe } from "../api/user";
import { getBrokerAllowed, getBrokerStatus } from "../api/broker";
import { getPositions, syncPositions } from "../api/positions";
import BrandMark from "../components/BrandMark";

export default function Dashboard() {
    const [username, setUsername] = useState("");
    const [watchlist, setWatchlist] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // Stored from GET /broker/allowed, which checks username
    const [brokerAllowed, setBrokerAllowed] = useState(false);
    const [connected, setConnected] = useState(false);
    const [positions, setPositions] = useState([]);
    // positionsLoading: Initially true, only false once there is data to show
    const [positionsLoading, setPositionsLoading] = useState(false);
    // positionsError: the connected check or the initial sync that
    // follows a successful allow-check genuinely failed. Only set by
    // the useEffect syncs, not background syncs
    const [positionsError, setPositionsError] = useState(false);
    // positionsSyncing: a live reconciliation against the broker is in
    // flight (either the automatic background one on load, or the manual
    // "Sync positions" button). Drives small indicator only
    const [positionsSyncing, setPositionsSyncing] = useState(false);

    // Guards state updates from in-flight requests (the background sync
    // in particular) that resolve after the component's gone - same
    // "cancelled" idea the load effect below uses locally, but shared
    // since syncPositionsInBackground can outlive that effect. This is
    // important for strict mode where everything is mounted/unmounted twice
    //
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

    // Reconcile live provider without touching what's on screen. Failures
    // here are swallowed since the display is already a good fallback, and
    // the user shouldn't have to react to a background refresh
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

            // Cheap DB so can be fired off immediately. Allows the last
            // stored positions to be displayed immediately, and swallows any errors
            // (like for non-allowed 404s)
            const positionsPromise = getPositions().catch(() => null);

            let allowed = false;
            try {
                const [meRes, watchlistRes, allowedRes] = await Promise.all([
                    getMe(),
                    getWatchlist(),
                    getBrokerAllowed(),
                ]);
                if (cancelled) return;
                setUsername(meRes.data.username);
                setWatchlist(watchlistRes.data.items);
                allowed = allowedRes.data.allowed;
                setBrokerAllowed(allowed);
            } catch {
                if (!cancelled) setError("Couldn't load your dashboard. Try refreshing.");
            } finally {
                if (!cancelled) setLoading(false);
            }

            if (!allowed || cancelled) return;
            const positionsRes = await positionsPromise; // Only trust await after allow check passes
            const stored = (allowed && positionsRes) ? positionsRes.data.items : [];
            setPositions(stored);

            // Broker/positions is a separate, best-effort load that only
            // starts once the allow-check above already came back true -
            try {
                const statusRes = await getBrokerStatus();
                if (cancelled) return;
                setConnected(statusRes.data.connected);

                if (statusRes.data.connected) {
                    // `stored` is the "last known state" fetched above,
                    // already on screen by now. On subsequent loads that represents
                    // a real past state of positions, but on the initial load
                    // there'll be nothing and it'll be hidden under a full spinner
                    if (stored.length === 0) {
                        // True initial load: nothing to show yet, so keep
                        // the whole section spinning until the first real
                        // sync completes, and surface a genuine failure here
                        setPositionsLoading(true);
                        try {
                            const syncRes = await syncPositions();
                            if (cancelled) return;
                            setPositions(syncRes.data.items);
                            setPositionsError(false);
                        } catch {
                            if (!cancelled) setPositionsError(true);
                        } finally {
                            if (!cancelled) setPositionsLoading(false);
                        }
                    } else {
                        // Refresh/subsequent login: last known state is
                        // already on screen, so just reconcile quietly 
                        syncPositionsInBackground();
                    }
                }
            } catch {
                if (!cancelled) setPositionsError(true);
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

    // Called by PositionDetail/WatchlistDetail after a successful force
    // price refresh. Patches the matching row in place here - the single
    // array both the sidebar and the detail panel's `item` prop are
    // derived from - rather than the detail component holding its own
    // "forced price" state that only it can see. That local-state version
    // is what caused the price to look stuck: the sidebar reads straight
    // from positions/watchlist and never knew a refresh happened, and
    // switching to a different row and back re-resolved `item` from the
    // still-stale array, discarding the local override.
    const handlePositionPriceUpdate = (instrumentId, priceUpdate) => {
        setPositions((prev) =>
            prev.map((p) => (p.instrumentId === instrumentId ? { ...p, ...priceUpdate } : p))
        );
    };

    const handleWatchlistPriceUpdate = (instrumentId, priceUpdate) => {
        setWatchlist((prev) =>
            prev.map((w) => (w.instrumentId === instrumentId ? { ...w, ...priceUpdate } : w))
        );
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
                                        disabled={positionsSyncing || positionsLoading}
                                    >
                                        {(positionsSyncing || positionsLoading) ? "Syncing..." : "Sync positions"}
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
                                positions={positions}
                                positionsLoading={positionsLoading}
                                positionsError={positionsError}
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
                                <DetailPanel
                                    selected={activeDetail}
                                    onRemoveWatchlistItem={handleRemove}
                                    onPositionPriceUpdate={handlePositionPriceUpdate}
                                    onWatchlistPriceUpdate={handleWatchlistPriceUpdate}
                                />
                            </main>
                        </div>
                    </>
                )}
            </div>
        </>
    );
}
