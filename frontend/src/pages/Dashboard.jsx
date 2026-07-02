import { useEffect, useState } from "react";
import api from "../api/axios";
import Watchlist from "../components/Watchlist";
import { addTicker, getWatchlist, removeTicker } from "../api/watchlist";

export default function Dashboard() {
    const [username, setUsername] = useState("");
    const [watchlist, setWatchlist] = useState([]);
    const [newTicker, setNewTicker] = useState("");

    useEffect(() => {
        loadMe();
        loadWatchlist();
    }, []);

    const loadMe = async () => {
        const res = await api.get("/me");
        setUsername(res.data.username);
    };

    const loadWatchlist = async () => {
        const res = await getWatchlist();
        setWatchlist(res.data.tickers);
    };

    const handleAdd = async () => {
        if (!newTicker) return;

        await addTicker(newTicker);
        setNewTicker("");
        loadWatchlist();
    };

    const handleRemove = async (ticker) => {
        await removeTicker(ticker);
        loadWatchlist();
    };

    const logout = () => {
        localStorage.removeItem("token");
        window.location.href = "/login";
    };

    return (
        <div>
            <h1>Dashboard</h1>

            <h2>Welcome, {username}</h2>

            <button onClick={logout}>
                Logout
            </button>

            <div>
                <input
                    placeholder="Add ticker (AAPL)"
                    value={newTicker}
                    onChange={(e) => setNewTicker(e.target.value.toUpperCase())}
                />
                <button onClick={handleAdd}>Add</button>
            </div>

            <Watchlist
                tickers={watchlist}
                onRemove={handleRemove}
            />
        </div>
    );
}