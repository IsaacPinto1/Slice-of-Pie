import { useEffect, useRef, useState } from "react";
import PortfolioSidebar from "../components/PortfolioSidebar";
import DetailPanel from "../components/DetailPanel";
import TickerSearch from "../components/TickerSearch";
import { addTicker, getWatchlist, removeTicker } from "../api/watchlist";
import { createInstrument } from "../api/instruments";
import { getMe } from "../api/user";
import { getBrokerAllowed, getBrokerStatus } from "../api/broker";
import { syncPositions } from "../api/positions";
import BrandMark from "../components/BrandMark";

export default function Dashboard() {
    const [username, setUsername] = useState("");
    const [watchlist, setWatchlist] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // Broker/positions state. brokerAllowed comes from GET /broker/allowed
    // - a cheap env-var check fetched alongside /me and /watchlist below,
    // so the Positions section can render (as a skeleton) in the same
    // paint as the watchlist instead of waiting on a slower round trip.
    const [brokerAllowed, setBrokerAllowed] = useState(false);
    const [connected, setConnected] = useState(false);
    const [positions, setPositions] = useState([]);
    // positionsLoading: true from the moment the allow-check comes back
    // true through the whole connected-check-then-initial-sync chain
    // below, so the section shows nothing but a spinner until there's a
    // real result to show - see the load effect's second phase.
    const [positionsLoading, setPositionsLoading] = useState(false);
    // positionsError: the connected check or the initial sync that
    // follows a successful allow-check genuinely failed - distinct from
    // "loaded fine, there's just nothing there yet". Only ever set by the
    // second effect phase below - the background sync a later manual
    // "Sync positions" click kicks off deliberately swallows its own
    // failures (see syncPositionsInBackground) and never touches this.
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

            // Broker/positions is a separate, best-effort load that only
            // starts once the allow-check above already came back true -
            // this is the live-data phase, on top of the skeleton the
            // Positions section is already showing. Deliberately does the
            // initial reconcile (syncPositions) itself here, under the
            // same spinner, rather than reading whatever's in the DB and
            // quietly reconciling afterward (that's still exactly what
            // syncPositionsInBackground/the "Sync positions" button do for
            // every subsequent sync) - on this very first load the DB can
            // easily be empty or stale, and revealing that before the live
            // reconcile catches up produced a flash of "No positions
            // synced yet" right before the real data popped in. Waiting
            // here means the section only ever shows one thing after the
            // spinner: the actual result. A failure anywhere in this
            // chain - the connected check or the sync itself - leaves the
            // section showing "Failed to load positions" (positionsError)
            // rather than surfacing a dashboard-wide error or hiding the
            // section - the allow-check already established the user
            // should see it.
            setPositionsLoading(true);
            try {
                const statusRes = await getBrokerStatus();
                if (cancelled) return;
                setConnected(statusRes.data.connected);

                if (statusRes.data.connected) {
                    const syncRes = await syncPositions();
                    if (cancelled) return;
                    setPositions(syncRes.data.items);
                    setPositionsError(false);
                }
            } catch {
                if (!cancelled) setPositionsError(true);
            } finally {
                if (!cancelled) setPositionsLoading(false);
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
                            {brokerAllowed && connected && !positionsLoading && (
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
                                <DetailPanel selected={activeDetail} onRemoveWatchlistItem={handleRemove} />
                            </main>
                        </div>
                    </>
                )}
            </div>
        </>
    );
}
