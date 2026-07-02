import { useEffect, useState } from "react";
import api from "../api/axios";

export default function Dashboard() {
    const [username, setUsername] = useState("");

    useEffect(() => {
        const fetchMe = async () => {
            try {
                const res = await api.get("/me");
                setUsername(res.data.username);
            } catch (err) {
                console.error(err);
            }
        };

        fetchMe();
    }, []);

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
        </div>
    );
}