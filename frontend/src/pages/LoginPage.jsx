import { useState } from "react";
import { login } from "../api/auth";
import { useNavigate, useLocation, Link, Navigate } from "react-router-dom";
import BrandMark from "../components/BrandMark";

export default function LoginPage() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const justRegistered = location.state?.registered;

    // Already signed in - no reason to show the login form again.
    if (localStorage.getItem("token")) {
        return <Navigate to="/dashboard" replace />;
    }

    const handleLogin = async (e) => {
        e.preventDefault();
        if (!username || !password) return;

        setError("");
        setSubmitting(true);
        try {
            const res = await login(username, password);
            localStorage.setItem("token", res.data.token);
            navigate("/dashboard");
        } catch (err) {
            const status = err.response?.status;
            setError(
                status === 401
                    ? "Incorrect username or password."
                    : "Couldn't log in. Please try again."
            );
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="auth-shell">
            <div className="auth-card">
                <div className="brand">
                    <BrandMark />
                    <span className="brand-name">Slice of Pie</span>
                </div>

                <h2>Welcome back</h2>
                <p className="auth-subtitle">Log in to see your watchlist</p>

                {justRegistered && !error && (
                    <div className="banner success">Account created. Log in to continue.</div>
                )}
                {error && <div className="banner error">{error}</div>}

                <form onSubmit={handleLogin}>
                    <div className="field">
                        <label htmlFor="username">Username</label>
                        <input
                            id="username"
                            name="username"
                            type="text"
                            autoComplete="username"
                            placeholder="jane_investor"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                        />
                    </div>

                    <div className="field">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            name="password"
                            type="password"
                            autoComplete="current-password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>

                    <button type="submit" disabled={submitting}>
                        {submitting ? "Logging in..." : "Log in"}
                    </button>
                </form>

                <p className="auth-footer">
                    Don't have an account? <Link to="/register">Register here</Link>
                </p>
            </div>
        </div>
    );
}
