import { useEffect, useRef, useState } from "react";
import { searchInstruments } from "../api/instruments";

// How long to wait after the user stops typing before firing a search.
// Finnhub's free tier allows 60 calls/minute; a 400ms debounce means even
// someone typing continuously can't realistically trigger more than a
// couple of requests per second, so this comfortably stays inside that
// limit even with several people searching at once.
const DEBOUNCE_MS = 400;

export default function TickerSearch({ onSelect, disabled }) {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);
    const [isOpen, setIsOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [highlightedIndex, setHighlightedIndex] = useState(-1);
    const [selecting, setSelecting] = useState(false);

    const containerRef = useRef(null);
    const requestIdRef = useRef(0);

    const handleQueryChange = (value) => {
        setQuery(value);
        if (!value.trim()) {
            // Nothing to search - clear immediately instead of waiting on
            // the debounce timer or doing it inside the effect body.
            setResults([]);
            setLoading(false);
            setError("");
            requestIdRef.current += 1; // invalidate any in-flight request
        } else {
            setLoading(true);
            setError("");
        }
    };

    // Debounced search - only fires DEBOUNCE_MS after the user stops typing.
    // Deliberately does no synchronous setState of its own (loading/error are
    // set eagerly in handleQueryChange) - only the async timer callback below
    // updates state, which is the pattern react-hooks/set-state-in-effect wants.
    useEffect(() => {
        const trimmed = query.trim();
        if (!trimmed) return;

        const timer = setTimeout(async () => {
            const requestId = ++requestIdRef.current;
            try {
                const res = await searchInstruments(trimmed);
                if (requestId !== requestIdRef.current) return; // stale response
                setResults(res.data);
                setIsOpen(true);
                setHighlightedIndex(-1);
            } catch {
                if (requestId !== requestIdRef.current) return;
                setResults([]);
                setError("Couldn't search right now. Try again.");
            } finally {
                if (requestId === requestIdRef.current) setLoading(false);
            }
        }, DEBOUNCE_MS);

        return () => clearTimeout(timer);
    }, [query]);

    // Close the dropdown on outside click.
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (containerRef.current && !containerRef.current.contains(e.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleSelect = async (result) => {
        if (selecting) return;
        setSelecting(true);
        setError("");
        try {
            await onSelect(result);
            setQuery("");
            setResults([]);
            setIsOpen(false);
        } catch {
            setError(`Couldn't add ${result.ticker}. Try again.`);
        } finally {
            setSelecting(false);
        }
    };

    const handleKeyDown = (e) => {
        if (!isOpen || results.length === 0) return;

        if (e.key === "ArrowDown") {
            e.preventDefault();
            setHighlightedIndex((i) => (i + 1) % results.length);
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setHighlightedIndex((i) => (i <= 0 ? results.length - 1 : i - 1));
        } else if (e.key === "Enter") {
            if (highlightedIndex >= 0) {
                e.preventDefault();
                handleSelect(results[highlightedIndex]);
            }
        } else if (e.key === "Escape") {
            setIsOpen(false);
        }
    };

    return (
        <div className="ticker-search-wrap" ref={containerRef}>
            <div className="ticker-search-input-wrap">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                    id="ticker-search-input"
                    type="text"
                    aria-label="Search for a ticker or company name"
                    placeholder="Ticker or company name (e.g. AAPL)"
                    value={query}
                    disabled={disabled || selecting}
                    onChange={(e) => handleQueryChange(e.target.value)}
                    onFocus={() => results.length > 0 && setIsOpen(true)}
                    onKeyDown={handleKeyDown}
                    autoComplete="off"
                />
                {loading && <span className="spinner ticker-search-spinner" />}
            </div>

            {isOpen && results.length > 0 && (
                <ul className="ticker-search-dropdown" role="listbox">
                    {results.map((result, index) => (
                        <li key={result.ticker}>
                            <button
                                type="button"
                                role="option"
                                aria-selected={index === highlightedIndex}
                                className={`ticker-search-option${index === highlightedIndex ? " highlighted" : ""}`}
                                disabled={selecting}
                                onMouseEnter={() => setHighlightedIndex(index)}
                                onClick={() => handleSelect(result)}
                            >
                                <span className="ticker-search-option-ticker">{result.ticker}</span>
                                <span className="ticker-search-option-sep">-</span>
                                <span className="ticker-search-option-name">{result.name}</span>
                            </button>
                        </li>
                    ))}
                </ul>
            )}

            {isOpen && !loading && results.length === 0 && query.trim() && !error && (
                <div className="ticker-search-dropdown ticker-search-empty">
                    No matches for "{query.trim()}"
                </div>
            )}

            {error && <p className="field-error">{error}</p>}
        </div>
    );
}
