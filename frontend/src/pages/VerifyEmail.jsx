import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authApi } from '../api/auth.js';

export default function VerifyEmail() {
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState('verifying');
    const [message, setMessage] = useState('');
    const [resendEmail, setResendEmail] = useState('');
    const [resendStatus, setResendStatus] = useState('idle');
    const token = searchParams.get('token');

    useEffect(() => {
        if (!token) {
            setStatus('error');
            setMessage('Verification token is missing.');
            return;
        }

        authApi
            .verifyEmail(token)
            .then(() => {
                setStatus('success');
                setMessage('Email verified successfully! You can now log in.');
            })
            .catch((err) => {
                setStatus('error');
                setMessage(err.response?.data?.message || 'Verification failed.');
            });
    }, [token]);

    const handleResend = async (e) => {
        e.preventDefault();
        if (!resendEmail) return;

        setResendStatus('loading');
        try {
            await authApi.resendVerification(resendEmail);
            setResendStatus('success');
            setMessage('New verification email sent. Check your inbox (and spam folder).');
        } catch (err) {
            setResendStatus('error');
            setMessage(err.response?.data?.message || 'Failed to resend verification email.');
        }
    };

    return (
        <div className="card" style={{ maxWidth: 500, margin: '0 auto', textAlign: 'center' }}>
            {status === 'verifying' && <div className="loading">Verifying your email...</div>}
            {status === 'success' && (
                <>
                    <div className="success">{message}</div>
                    <Link to="/login" className="btn btn-primary mt-4">
                        Go to Login
                    </Link>
                </>
            )}
            {status === 'error' && (
                <>
                    <div className="error">{message}</div>
                    <form onSubmit={handleResend} className="mt-4">
                        <div className="form-group">
                            <label>Email</label>
                            <input
                                type="email"
                                value={resendEmail}
                                onChange={(e) => setResendEmail(e.target.value)}
                                placeholder="Enter your email to resend verification"
                                required
                            />
                        </div>
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={resendStatus === 'loading'}
                        >
                            {resendStatus === 'loading' ? 'Sending...' : 'Resend Verification Email'}
                        </button>
                    </form>
                    <Link to="/login" className="btn btn-secondary mt-4">
                        Go to Login
                    </Link>
                </>
            )}
        </div>
    );
}
