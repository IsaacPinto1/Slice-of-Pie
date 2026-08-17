import { useMemo } from "react";
import SidebarSection from "./SidebarSection";
import SidebarItemCard from "./SidebarItemCard";

// Sum of quantity * price across all synced positions. Purely a
// client-side derivation of data already on hand (PositionItemResponse
// carries both fields) - no extra endpoint needed, and it recomputes
// whenever the positions list changes.
function usePositionsValue(positions) {
    return useMemo(() => {
        const total = positions.reduce((sum, item) => {
            const quantity = Number(item.quantity);
            const price = Number(item.price);
            if (!Number.isFinite(quantity) || !Number.isFinite(price)) return sum;
            return sum + quantity * price;
        }, 0);
        return `$${total.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }, [positions]);
}

// Percentage change between the live price and cost basis, both of which
// PositionItemResponse already carries - no extra endpoint needed. A
// costBasis of exactly 0 (a position that predates cost-basis tracking
// and hasn't been re-synced yet) can't support a meaningful percentage,
// so it's treated the same as a missing value rather than reported as a
// misleading +Infinity/undefined-driven number.
function percentChange(item) {
    const price = Number(item.price);
    const costBasis = Number(item.costBasis);
    if (!Number.isFinite(price) || !Number.isFinite(costBasis) || costBasis === 0) return null;
    return ((price - costBasis) / costBasis) * 100;
}

// Positions ranked by percentChange, best performers first. Positions
// without a usable percentage (see percentChange above) sort to the
// bottom rather than being dropped, so a stale/never-synced cost basis
// doesn't hide the position itself.
function usePositionsRankedByChange(positions) {
    return useMemo(() => {
        return positions
            .map((item) => ({ item, change: percentChange(item) }))
            .sort((a, b) => {
                if (a.change == null && b.change == null) return 0;
                if (a.change == null) return 1;
                if (b.change == null) return -1;
                return b.change - a.change;
            })
            .map(({ item, change }) => ({ ...item, percentChange: change }));
    }, [positions]);
}

// Sidebar for the sidebar + focused-item layout: a collapsible
// "Positions" section stacked on top of a collapsible "Watchlist"
// section, each holding small ticker+price cards. Selecting a card just
// reports {type, instrumentId} up to Dashboard - it owns the actual
// selected item, resolved fresh from positions/watchlist on every render.
export default function PortfolioSidebar({
    brokerAllowed,
    connected,
    positions,
    positionsLoading,
    positionsSyncing,
    watchlist,
    selected,
    onSelect,
    positionsCollapsed,
    onTogglePositions,
    watchlistCollapsed,
    onToggleWatchlist,
}) {
    const positionsValue = usePositionsValue(positions);
    const rankedPositions = usePositionsRankedByChange(positions);

    return (
        <nav className="sidebar" aria-label="Positions and watchlist">
            {brokerAllowed && connected && (
                <SidebarSection
                    title="Positions"
                    count={positions.length}
                    total={positions.length > 0 && !positionsLoading ? positionsValue : null}
                    syncing={positionsSyncing}
                    collapsed={positionsCollapsed}
                    onToggleCollapse={onTogglePositions}
                >
                    {positionsLoading ? (
                        <div className="loading-row sidebar-loading">
                            <span className="spinner" />
                        </div>
                    ) : positions.length === 0 ? (
                        <p className="sidebar-empty">No positions synced yet.</p>
                    ) : (
                        rankedPositions.map((item) => (
                            <SidebarItemCard
                                key={item.instrumentId}
                                item={item}
                                active={selected?.type === "position" && selected.instrumentId === item.instrumentId}
                                onSelect={() => onSelect({ type: "position", instrumentId: item.instrumentId })}
                            />
                        ))
                    )}
                </SidebarSection>
            )}

            <SidebarSection
                title="Watchlist"
                count={watchlist.length}
                collapsed={watchlistCollapsed}
                onToggleCollapse={onToggleWatchlist}
            >
                {watchlist.length === 0 ? (
                    <p className="sidebar-empty">Your watchlist is empty.</p>
                ) : (
                    watchlist.map((item) => (
                        <SidebarItemCard
                            key={item.instrumentId}
                            item={item}
                            active={selected?.type === "watchlist" && selected.instrumentId === item.instrumentId}
                            onSelect={() => onSelect({ type: "watchlist", instrumentId: item.instrumentId })}
                        />
                    ))
                )}
            </SidebarSection>
        </nav>
    );
}
