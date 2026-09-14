import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { venuesApi } from '../api/venues.js';
import { eventsApi } from '../api/events.js';

export default function VenueSchedule() {
    const { venueId } = useParams();
    const navigate = useNavigate();
    const [venue, setVenue] = useState(null);
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadVenueAndEvents();
    }, [venueId]);

    const loadVenueAndEvents = async () => {
        setLoading(true);
        setError(null);
        try {
            const [venueRes, eventsRes] = await Promise.all([
                venuesApi.getVenue(venueId),
                eventsApi.getEventsByVenue(venueId, 0, 100, 'startsAt'),
            ]);
            setVenue(venueRes.data);
            setEvents(eventsRes.data.content || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Не вдалося завантажити розклад');
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (instant) => {
        if (!instant) return '';
        const d = new Date(instant);
        return d.toLocaleDateString('uk-UA', { day: 'numeric', month: 'long' });
    };

    const formatTime = (instant) => {
        if (!instant) return '';
        const d = new Date(instant);
        return d.toLocaleTimeString('uk-UA', { hour: '2-digit', minute: '2-digit' });
    };

    const groupByDate = (events) => {
        const groups = {};
        events.forEach((event) => {
            const dateKey = event.startsAt ? new Date(event.startsAt).toDateString() : 'unknown';
            if (!groups[dateKey]) groups[dateKey] = [];
            groups[dateKey].push(event);
        });
        return groups;
    };

    const grouped = groupByDate(events);

    if (loading) {
        return <div className="loading">Завантаження розкладу...</div>;
    }

    return (
        <div className="container">
            <button className="btn btn-secondary mb-4" onClick={() => navigate(-1)}>
                ← Назад
            </button>

            {venue && (
                <div className="venue-header-block mb-4">
                    <h1>{venue.name}</h1>
                    <p className="text-muted">{venue.address}</p>
                </div>
            )}

            {error && <div className="error">{error}</div>}

            {Object.keys(grouped).length === 0 && !error && (
                <p className="text-muted" style={{ textAlign: 'center', padding: '48px 0' }}>
                    На цьому тижні подій немає.
                </p>
            )}

            {Object.entries(grouped).map(([dateKey, dateEvents]) => (
                <div key={dateKey} className="schedule-day mb-4">
                    <h3 className="schedule-date">{formatDate(dateEvents[0].startsAt)}</h3>
                    <div className="schedule-grid">
                        {dateEvents.map((event) => (
                            <div key={event.id} className="schedule-card">
                                <div className="schedule-poster">
                                    <img
                                        src={
                                            event.posterUrl ||
                                            `https://placehold.co/120x180/e53e3e/ffffff?text=${encodeURIComponent(event.title)}`
                                        }
                                        alt={event.title}
                                        onError={(e) => {
                                            e.target.src = `https://placehold.co/120x180/e53e3e/ffffff?text=${encodeURIComponent(event.title)}`;
                                        }}
                                    />
                                </div>
                                <div className="schedule-info">
                                    <h4 className="schedule-title">{event.title}</h4>
                                    <p className="schedule-desc">{event.description}</p>
                                    <div className="schedule-times">
                                        <Link
                                            to={`/events/${event.id}`}
                                            className="time-btn"
                                        >
                                            {formatTime(event.startsAt)}
                                        </Link>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            ))}
        </div>
    );
}
