export default function BrandMark({ size = 26 }) {
    return (
        <svg
            className="brand-mark"
            width={size}
            height={size}
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
        >
            <circle cx="16" cy="16" r="14" fill="var(--accent-bg)" stroke="var(--accent)" strokeWidth="1.5" />
            <path d="M16 16 L16 4 A12 12 0 0 1 26.39 22 Z" fill="var(--accent)" />
        </svg>
    );
}
