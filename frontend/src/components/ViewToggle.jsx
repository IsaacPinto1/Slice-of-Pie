const OPTIONS = [
    { value: "both", label: "Both" },
    { value: "positions", label: "Positions" },
    { value: "watchlist", label: "Watchlist" },
];

// Plain, uncontrolled-by-anything-else segmented control. Kept generic
// (value/onChange) rather than positions/watchlist-specific so it isn't
// tied to Dashboard's state shape.
export default function ViewToggle({ value, onChange }) {
    return (
        <div className="view-toggle" role="tablist" aria-label="Choose what to show">
            {OPTIONS.map((option) => (
                <button
                    key={option.value}
                    type="button"
                    role="tab"
                    aria-selected={value === option.value}
                    className={`view-toggle-option${value === option.value ? " active" : ""}`}
                    onClick={() => onChange(option.value)}
                >
                    {option.label}
                </button>
            ))}
        </div>
    );
}
