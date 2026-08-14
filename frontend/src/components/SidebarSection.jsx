// Generic collapsible section used for both the "Positions" and
// "Watchlist" groups in the sidebar. Kept content-agnostic (title/count
// in, children out) rather than positions/watchlist-specific, same
// reasoning as the old ViewToggle being value/onChange generic.
export default function SidebarSection({ title, count, collapsed, onToggleCollapse, children }) {
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
                {typeof count === "number" && (
                    <span className="sidebar-section-count">{count}</span>
                )}
            </button>

            {!collapsed && <div className="sidebar-section-body">{children}</div>}
        </div>
    );
}
