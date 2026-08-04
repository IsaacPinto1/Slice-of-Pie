import { useState } from "react";
import { register } from "../api/auth";
import { useNavigate, Link, Navigate } from "react-router-dom";
import BrandMark from "../components/BrandMark";

export default function RegisterPage() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();

    if (localStorage.getItem("token")) {
        return <Navigate to="/dashboard" replace />;
    }

    const passwordsMismatch = confirmPassword.length > 0 && password !== confirmPassword;

    const handleRegister = async (e) => {
        e.preventDefault();
        if (!username || !password || passwordsMismatch) return;

        setError("");
        setSubmitting(true);
        try {
            await register(username, password);
            navigate("/login", { state: { registered: true } });
        } catch (err) {
            const status = err.response?.status;
            setError(
                status === 409
                    ? "That username is already taken."
                    : "Couldn't register. Please try again."
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

                <h2>Create an account</h2>
                <p className="auth-subtitle">Track your watchlist and investing theses</p>

                {error && <div className="banner error">{error}</div>}

                <form onSubmit={handleRegister}>
                    <div className="field">
                        <label htmlFor="username">Username</label>
                        <input
                            id="username"
                            name="username"
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
                            autoComplete="new-password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>

                    <div className="field">
                        <label htmlFor="confirm-password">Confirm password</label>
                        <input
                            id="confirm-password"
                            name="confirm-password"
                            type="password"
                            autoComplete="new-password"
                            placeholder="••••••••"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                        />
                        {passwordsMismatch && (
                            <p className="field-error">Passwords don't match.</p>
                        )}
                    </div>

                    <button type="submit" disabled={submitting || passwordsMismatch}>
                        {submitting ? "Creating account..." : "Register"}
                    </button>
                </form>

                <p className="auth-footer">
                    Already have an account? <Link to="/login">Log in here</Link>
                </p>
            </div>
        </div>
    );
}
