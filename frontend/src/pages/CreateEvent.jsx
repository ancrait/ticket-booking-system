import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { venuesApi } from '../api/venues.js';
import { eventsApi } from '../api/events.js';

export default function CreateEvent() {
    const navigate = useNavigate();
    const [venues, setVenues] = useState([]);
    const [halls, setHalls] = useState([]);
    const [form, setForm] = useState({
        title: '',
        description: '',
        posterUrl: '',
        startsAt: '',
        endsAt: '',
        hallId: '',
    });
    const [selectedVenue, setSelectedVenue] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        loadVenues();
    }, []);

    const loadVenues = async () => {
        try {
            const { data } = await venuesApi.getVenues();
            setVenues(data.content || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load venues');
        }
    };

    const handleVenueChange = async (venueId) => {
        setSelectedVenue(venueId);
        setForm({ ...form, hallId: '' });
        if (!venueId) {
            setHalls([]);
            return;
        }
        try {
            const { data } = await venuesApi.getHalls(venueId);
            setHalls(data || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load halls');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            await eventsApi.createEvent({
                ...form,
                startsAt: new Date(form.startsAt).toISOString(),
                endsAt: new Date(form.endsAt).toISOString(),
            });
            navigate('/organizer/events');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create event');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card" style={{ maxWidth: 600, margin: '0 auto' }}>
            <h2>Create Event</h2>
            {error && <div className="error">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Title</label>
                    <input
                        type="text"
                        value={form.title}
                        onChange={(e) => setForm({ ...form, title: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>Description</label>
                    <textarea
                        value={form.description}
                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                        rows={3}
                    />
                </div>
                <div className="form-group">
                    <label>Poster URL</label>
                    <input
                        type="url"
                        value={form.posterUrl}
                        onChange={(e) => setForm({ ...form, posterUrl: e.target.value })}
                    />
                </div>
                <div className="form-group">
                    <label>Starts At</label>
                    <input
                        type="datetime-local"
                        value={form.startsAt}
                        onChange={(e) => setForm({ ...form, startsAt: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>Ends At</label>
                    <input
                        type="datetime-local"
                        value={form.endsAt}
                        onChange={(e) => setForm({ ...form, endsAt: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>Venue</label>
                    <select
                        value={selectedVenue}
                        onChange={(e) => handleVenueChange(e.target.value)}
                        required
                    >
                        <option value="">Select venue</option>
                        {venues.map((venue) => (
                            <option key={venue.id} value={venue.id}>
                                {venue.name} ({venue.city})
                            </option>
                        ))}
                    </select>
                </div>
                <div className="form-group">
                    <label>Hall</label>
                    <select
                        value={form.hallId}
                        onChange={(e) => setForm({ ...form, hallId: e.target.value })}
                        required
                        disabled={!selectedVenue}
                    >
                        <option value="">Select hall</option>
                        {halls.map((hall) => (
                            <option key={hall.id} value={hall.id}>
                                {hall.name}
                            </option>
                        ))}
                    </select>
                </div>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                    {loading ? 'Creating...' : 'Create Event'}
                </button>
            </form>
        </div>
    );
}
