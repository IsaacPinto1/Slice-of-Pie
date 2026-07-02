import { useEffect, useState } from "react";
import { getThesis, saveThesis } from "../api/thesis";
import ThesisEditor from "./ThesisEditor";

export default function WatchlistItem({ ticker, onRemove }) {
    const [thesis, setThesis] = useState("");

    useEffect(() => {
        loadThesis();
    }, [ticker]);

    const loadThesis = async () => {
        try {
            const res = await getThesis(ticker);
            setThesis(res.data.content);
        } catch (err) {
            setThesis("");
        }
    };

    return (
        <div style={{ border: "1px solid #ccc", margin: 10, padding: 10 }}>
            <h4>{ticker}</h4>

            <ThesisEditor
                ticker={ticker}
                thesis={thesis}
                setThesis={setThesis}
            />

            <button onClick={() => onRemove(ticker)}>
                Remove
            </button>
        </div>
    );
}