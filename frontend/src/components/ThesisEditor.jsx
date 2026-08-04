import { useState } from "react";
import { saveThesis } from "../api/thesis";

export default function ThesisEditor({ instrumentId, thesis, setThesis }) {
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    // Track the text as it was at the last successful save, so the "Saved"
    // indicator is derived instead of needing its own effect to reset it.
    const [lastSavedValue, setLastSavedValue] = useState(null);

    const isSaved = lastSavedValue !== null && lastSavedValue === thesis;

    const handleSave = async () => {
        setSaving(true);
        setError("");
        try {
            await saveThesis(instrumentId, thesis);
            setLastSavedValue(thesis);
        } catch {
            setError("Couldn't save. Try again.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="thesis-editor">
            <textarea
                value={thesis}
                onChange={(e) => setThesis(e.target.value)}
                rows={4}
                placeholder="Write your thesis..."
            />

            <div className="thesis-editor-footer">
                {error && <span style={{ color: "var(--danger)", fontSize: 13 }}>{error}</span>}
                {!error && isSaved && <span className="inline-status">Saved</span>}
                {!error && !isSaved && <span />}

                <button className="small" onClick={handleSave} disabled={saving}>
                    {saving ? "Saving..." : "Save thesis"}
                </button>
            </div>
        </div>
    );
}
