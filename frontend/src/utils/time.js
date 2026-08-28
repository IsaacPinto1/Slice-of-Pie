// Compact "how long ago" formatting for a price's last-updated timestamp -
// shared between PositionDetail and WatchlistDetail so both agree on the
// same buckets/rounding instead of each rolling their own.
export function formatRelativeTime(isoString) {
    if (!isoString) return null;
    const updatedAt = new Date(isoString).getTime();
    if (Number.isNaN(updatedAt)) return null;

    const diffMs = Date.now() - updatedAt;
    if (diffMs < 0) return "just now";

    const diffMin = Math.round(diffMs / 60000);
    if (diffMin < 1) return "just now";
    if (diffMin < 60) return `${diffMin}m ago`;

    const diffHr = Math.round(diffMin / 60);
    if (diffHr < 24) return `${diffHr}h ago`;

    const diffDay = Math.round(diffHr / 24);
    return `${diffDay}d ago`;
}
