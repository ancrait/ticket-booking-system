import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { eventsApi } from '../api/events.js';

export default function Events() {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [filters, setFilters] = useState({ city: '', date: '' });

    useEffect(() => {
        loadEvents();
    }, [filters]);

    const loadEvents = async () => {
        setLoading(true);
        try {
            const { data } = await eventsApi.searchEvents(
                filters.city || undefined,
                filters.date || undefined
            );
            setEvents(data.content || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load events');
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (instant) => {
        if (!instant) return 'TBA';
        return new Date(instant).toLocaleString();
    };

    if (loading) {
        return <div className="loading">Loading events...</div>;
    }

    return (
        <div>
            <h2>Events</h2>
            <div className="card mb-4">
                <div className="flex gap-4" style={{ flexWrap: 'wrap' }}>
                    <div className="form-group" style={{ flex: 1, minWidth: 200 }}>
                        <label>City</label>
                        <input
                            type="text"
                            value={filters.city}
                            onChange={(e) => setFilters({ ...filters, city: e.target.value })}
                            placeholder="Search by city"
                        />
                    </div>
                    <div className="form-group" style={{ flex: 1, minWidth: 200 }}>
                        <label>Date</label>
                        <input
                            type="date"
                            value={filters.date}
                            onChange={(e) => setFilters({ ...filters, date: e.target.value })}
                        />
                    </div>
                </div>
            </div>

            {error && <div className="error">{error}</div>}

            <div className="grid grid-3">
                {events.map((event) => (
                    <div key={event.id} className="card event-card">
                        <div className="flex flex-between">
                            <h3>{event.title}</h3>
                            <span className={`status-badge status-${event.status?.toLowerCase()}`}>
                                {event.status}
                            </span>
                        </div>
                        <p>{event.description}</p>
                        <p className="text-muted">
                            {formatDate(event.startsAt)} — {formatDate(event.endsAt)}
                        </p>
                        <Link to={`/events/${event.id}`} className="btn btn-primary">
                            View Details
                        </Link>
                    </div>
                ))}
            </div>

            {events.length === 0 && !error && (
                <p className="text-muted" style={{ textAlign: 'center' }}>
                    No events found.
                </p>
            )}
        </div>
    );
}
