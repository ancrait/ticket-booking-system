import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { eventsApi } from '../api/events.js';
import { bookingsApi } from '../api/bookings.js';
import { paymentsApi } from '../api/payments.js';
import { StripeCheckout } from '../components/StripeCheckout.jsx';
import SeatMap from '../components/SeatMap.jsx';
import { useAuth } from '../context/AuthContext.jsx';

export default function EventDetails() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();
    const [event, setEvent] = useState(null);
    const [seats, setSeats] = useState([]);
    const [selectedSeats, setSelectedSeats] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [booking, setBooking] = useState(null);
    const [clientSecret, setClientSecret] = useState(null);
    const [processing, setProcessing] = useState(false);

    useEffect(() => {
        loadEventAndSeats();
    }, [id]);

    const loadEventAndSeats = async () => {
        setLoading(true);
        try {
            const [eventRes, seatsRes] = await Promise.all([
                eventsApi.getEvent(id),
                eventsApi.getEventSeats(id, 0, 500),
            ]);
            setEvent(eventRes.data);
            setSeats(seatsRes.data.content || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load event details');
        } finally {
            setLoading(false);
        }
    };

    const toggleSeat = (seat) => {
        if (seat.status !== 'FREE') return;

        setSelectedSeats((prev) => {
            const exists = prev.find((s) => s.id === seat.id);
            if (exists) {
                return prev.filter((s) => s.id !== seat.id);
            }
            return [...prev, seat];
        });
    };

    const handleBook = async () => {
        if (selectedSeats.length === 0) {
            setError('Please select at least one seat');
            return;
        }

        if (!isAuthenticated) {
            navigate('/login');
            return;
        }

        setProcessing(true);
        setError(null);

        try {
            const { data } = await bookingsApi.createBooking({
                eventId: id,
                bookingItemsRequests: selectedSeats.map((seat) => ({
                    eventSeatId: seat.id,
                })),
            });
            setBooking(data);
            const { data: paymentData } = await paymentsApi.initiate(data.id);
            setClientSecret(paymentData.clientSecret);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create booking');
        } finally {
            setProcessing(false);
        }
    };

    const handlePaymentSuccess = () => {
        navigate('/my-bookings');
    };

    const formatDate = (instant) => {
        if (!instant) return 'TBA';
        return new Date(instant).toLocaleString();
    };

    const totalPrice = selectedSeats.reduce((sum, seat) => sum + (seat.price || 0), 0);

    if (loading) {
        return <div className="loading">Loading event details...</div>;
    }

    if (!event) {
        return <div className="error">Event not found</div>;
    }

    return (
        <div>
            <div className="card mb-4">
                <div className="flex flex-between">
                    <h2>{event.title}</h2>
                    <span className={`status-badge status-${event.status?.toLowerCase()}`}>
                        {event.status}
                    </span>
                </div>
                <p>{event.description}</p>
                <p className="text-muted">
                    {formatDate(event.startsAt)} — {formatDate(event.endsAt)}
                </p>
            </div>

            {clientSecret ? (
                <StripeCheckout
                    clientSecret={clientSecret}
                    onSuccess={handlePaymentSuccess}
                    onCancel={() => setClientSecret(null)}
                />
            ) : (
                <>
                    <h3>Select Seats</h3>
                    {error && <div className="error">{error}</div>}

                    <SeatMap
                        seats={seats}
                        selectedSeats={selectedSeats}
                        onToggle={toggleSeat}
                    />

                    {seats.length === 0 && (
                        <p className="text-muted">No seats available for this event.</p>
                    )}

                    {selectedSeats.length > 0 && (
                        <div className="card">
                            <h3>Booking Summary</h3>
                            <p>Selected seats: {selectedSeats.length}</p>
                            <p>Total: {totalPrice.toFixed(2)} UAH</p>
                            <button
                                className="btn btn-primary"
                                onClick={handleBook}
                                disabled={processing}
                            >
                                {processing ? 'Processing...' : 'Book & Pay'}
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
