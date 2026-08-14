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
    watchlist,
    selected,
    onSelect,
    positionsCollapsed,
    onTogglePositions,
    watchlistCollapsed,
    onToggleWatchlist,
}) {
    const positionsValue = usePositionsValue(positions);

    return (
        <nav className="sidebar" aria-label="Positions and watchlist">
            {brokerAllowed && connected && (
                <SidebarSection
                    title="Positions"
                    count={positions.length}
                    total={positions.length > 0 && !positionsLoading ? positionsValue : null}
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
                        positions.map((item) => (
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
