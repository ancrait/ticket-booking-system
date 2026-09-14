import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { eventsApi } from '../api/events.js';

export default function OrganizerEvents() {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadEvents();
    }, []);

    const loadEvents = async () => {
        setLoading(true);
        try {
            const { data } = await eventsApi.getMyEvents();
            setEvents(data || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load events');
        } finally {
            setLoading(false);
        }
    };

    const handlePublish = async (id) => {
        try {
            await eventsApi.publishEvent(id);
            loadEvents();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to publish event');
        }
    };

    const handleCancel = async (id) => {
        try {
            await eventsApi.cancelEvent(id);
            loadEvents();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to cancel event');
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
            <div className="flex flex-between mb-4">
                <h2>My Events</h2>
                <Link to="/organizer/events/new" className="btn btn-primary">
                    Create Event
                </Link>
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
                        <div className="flex gap-2 mt-4">
                            {event.status === 'DRAFT' && (
                                <button
                                    className="btn btn-primary"
                                    onClick={() => handlePublish(event.id)}
                                >
                                    Publish
                                </button>
                            )}
                            {event.status === 'PUBLISHED' && (
                                <button
                                    className="btn btn-danger"
                                    onClick={() => handleCancel(event.id)}
                                >
                                    Cancel
                                </button>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            {events.length === 0 && !error && (
                <p className="text-muted" style={{ textAlign: 'center' }}>
                    You don't have any events yet.{' '}
                    <Link to="/organizer/events/new">Create one</Link>.
                </p>
            )}
        </div>
    );
}
