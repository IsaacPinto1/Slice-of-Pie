// Generic collapsible section used for both the "Positions" and
// "Watchlist" groups in the sidebar. Kept content-agnostic (title/count
// in, children out) rather than positions/watchlist-specific, same
// reasoning as the old ViewToggle being value/onChange generic.
//
// `syncing` is separate from whatever's happening in `children` - it's a
// small indicator next to the title (a background reconciliation is in
// flight) that never affects what's rendered below it, so the list this
// section holds is never blanked out while a sync runs.
export default function SidebarSection({ title, count, total, syncing, collapsed, onToggleCollapse, children }) {
    return (
        <div className="sidebar-section">
            <button
                type="button"
                className="sidebar-section-header"
                onClick={onToggleCollapse}
                aria-expanded={!collapsed}
            >
                <svg
                    className={`sidebar-chevron${collapsed ? " collapsed" : ""}`}
                    viewBox="0 0 24 24"
                    width="14"
                    height="14"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                >
                    <polyline points="6 9 12 15 18 9" />
                </svg>
                <span className="sidebar-section-title">{title}</span>
                {syncing && (
                    <span className="spinner sidebar-section-spinner" aria-label="Syncing" title="Syncing" />
                )}
                {total != null && (
                    <span className="sidebar-section-total">{total}</span>
                )}
                {typeof count === "number" && (
                    <span className="sidebar-section-count">{count}</span>
                )}
            </button>

            {!collapsed && <div className="sidebar-section-body">{children}</div>}
        </div>
    );
}
