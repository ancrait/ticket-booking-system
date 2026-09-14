import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth.js';
import { useAuth } from '../context/AuthContext.jsx';

export default function Login() {
    const [form, setForm] = useState({ email: '', password: '' });
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const [resendStatus, setResendStatus] = useState('idle');
    const { login } = useAuth();
    const navigate = useNavigate();

    const isEmailNotVerified = error && error.toLowerCase().includes('not verified');

    const handleResend = async () => {
        if (!form.email) return;
        setResendStatus('loading');
        try {
            await authApi.resendVerification(form.email);
            setResendStatus('success');
            setError('Verification email resent. Check your inbox (and spam folder).');
        } catch (err) {
            setResendStatus('error');
            setError(err.response?.data?.message || 'Failed to resend verification email.');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            const { data } = await authApi.login(form);
            login(data);
            navigate('/');
        } catch (err) {
            setError(err.response?.data?.message || 'Login failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card" style={{ maxWidth: 400, margin: '0 auto' }}>
            <h2>Login</h2>
            {error && <div className="error">{error}</div>}
            {isEmailNotVerified && (
                <button
                    type="button"
                    className="btn btn-secondary mb-4"
                    onClick={handleResend}
                    disabled={resendStatus === 'loading'}
                >
                    {resendStatus === 'loading' ? 'Sending...' : 'Resend Verification Email'}
                </button>
            )}
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Email</label>
                    <input
                        type="email"
                        value={form.email}
                        onChange={(e) => setForm({ ...form, email: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>Password</label>
                    <input
                        type="password"
                        value={form.password}
                        onChange={(e) => setForm({ ...form, password: e.target.value })}
                        required
                    />
                </div>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                    {loading ? 'Logging in...' : 'Login'}
                </button>
            </form>
            <p className="text-muted mt-4">
                Don't have an account? <Link to="/register">Register</Link>
            </p>
        </div>
    );
}
