import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { bookingsApi } from '../api/bookings.js';
import { paymentsApi } from '../api/payments.js';
import { StripeCheckout } from '../components/StripeCheckout.jsx';

export default function MyBookings() {
    const [bookings, setBookings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);
    const [payBookingId, setPayBookingId] = useState(null);
    const [clientSecret, setClientSecret] = useState(null);
    const [searchParams, setSearchParams] = useSearchParams();

    useEffect(() => {
        loadBookings();
    }, []);

    useEffect(() => {
        const redirectStatus = searchParams.get('redirect_status');
        const paymentIntent = searchParams.get('payment_intent');

        if (redirectStatus && paymentIntent) {
            if (redirectStatus === 'succeeded') {
                setSuccessMessage('Payment successful! Updating booking status...');
            } else if (redirectStatus === 'failed') {
                setError('Payment failed. Please try again.');
            }
            setSearchParams({}, { replace: true });
        }
    }, [searchParams, setSearchParams]);

    useEffect(() => {
        if (!successMessage) {
            return;
        }

        let attempts = 0;
        const interval = setInterval(() => {
            loadBookings(false);
            attempts++;
            if (attempts >= 10) {
                clearInterval(interval);
            }
        }, 3000);

        return () => clearInterval(interval);
    }, [successMessage]);



    const loadBookings = async (showLoading = true) => {
        if (showLoading) {
            setLoading(true);
        }
        try {
            const { data } = await bookingsApi.getMyBookings();
            setBookings(data.content || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load bookings');
        } finally {
            if (showLoading) {
                setLoading(false);
            }
        }
    };

    const handleCancel = async (id) => {
        try {
            await bookingsApi.cancelBooking(id);
            loadBookings();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to cancel booking');
        }
    };

    const handlePay = async (id) => {
        setError(null);
        setPayBookingId(id);
        try {
            const { data } = await paymentsApi.initiate(id);
            if (data.alreadyPaid) {
                setSuccessMessage('Payment already succeeded! Updating booking status...');
                handlePaymentSuccess();
            } else {
                setClientSecret(data.clientSecret);
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to initiate payment');
            setPayBookingId(null);
        }
    };

    const handlePaymentSuccess = () => {
        setClientSecret(null);
        setPayBookingId(null);
        loadBookings();
    };

    const formatDate = (instant) => {
        if (!instant) return 'TBA';
        return new Date(instant).toLocaleString();
    };

    if (loading) {
        return <div className="loading">Loading bookings...</div>;
    }

    return (
        <div>
            <h2>My Bookings</h2>
            {error && <div className="error">{error}</div>}
            {successMessage && <div className="success">{successMessage}</div>}

            {clientSecret && (
                <StripeCheckout
                    clientSecret={clientSecret}
                    onSuccess={handlePaymentSuccess}
                    onCancel={() => {
                        setClientSecret(null);
                        setPayBookingId(null);
                    }}
                />
            )}

            <div className="grid grid-3">
                {bookings.map((booking) => (
                    <div key={booking.id} className="card event-card">
                        <div className="flex flex-between">
                            <h3>{booking.eventTitle}</h3>
                            <span className={`status-badge status-${booking.status?.toLowerCase()}`}>
                                {booking.status}
                            </span>
                        </div>
                        <p>Total: {booking.totalPrice} UAH</p>
                        <p className="text-muted">Created: {formatDate(booking.createdAt)}</p>
                        <p className="text-muted">Expires: {formatDate(booking.expiresAt)}</p>
                        <div className="flex gap-2 mt-4">
                            {booking.status === 'PENDING' && (
                                <>
                                    <button
                                        className="btn btn-primary"
                                        onClick={() => handlePay(booking.id)}
                                        disabled={payBookingId === booking.id && !clientSecret}
                                    >
                                        Pay
                                    </button>
                                    <button
                                        className="btn btn-danger"
                                        onClick={() => handleCancel(booking.id)}
                                    >
                                        Cancel
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            {bookings.length === 0 && !error && (
                <p className="text-muted" style={{ textAlign: 'center' }}>
                    You don't have any bookings yet.
                </p>
            )}
        </div>
    );
}
