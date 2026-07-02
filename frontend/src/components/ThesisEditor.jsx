import { saveThesis } from "../api/thesis";

export default function ThesisEditor({ ticker, thesis, setThesis }) {

    const handleSave = async () => {
        await saveThesis(ticker, thesis);
        alert("Saved");
    };

    return (
        <div>
            <textarea
                value={thesis}
                onChange={(e) => setThesis(e.target.value)}
                rows={4}
                cols={50}
                placeholder="Write your thesis..."
            />

            <br />

            <button onClick={handleSave}>
                Save Thesis
            </button>
        </div>
    );
}