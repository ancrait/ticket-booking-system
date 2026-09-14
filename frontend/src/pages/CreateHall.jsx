import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { venuesApi, hallsApi } from '../api/venues.js';

export default function CreateHall() {
    const navigate = useNavigate();
    const [venues, setVenues] = useState([]);
    const [form, setForm] = useState({
        venueId: '',
        name: '',
        rowsCount: '',
        seatsPerRow: '',
    });
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

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            const { data } = await venuesApi.createHall(form.venueId, {
                name: form.name,
                rowsCount: Number(form.rowsCount),
                seatsPerRow: Number(form.seatsPerRow),
            });
            await hallsApi.generateSeats(data.id);
            navigate('/organizer');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create hall');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card" style={{ maxWidth: 500, margin: '0 auto' }}>
            <h2>Create Hall</h2>
            {error && <div className="error">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Venue</label>
                    <select
                        value={form.venueId}
                        onChange={(e) => setForm({ ...form, venueId: e.target.value })}
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
                    <label>Hall Name</label>
                    <input
                        type="text"
                        value={form.name}
                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>Rows Count</label>
                    <input
                        type="number"
                        min="1"
                        value={form.rowsCount}
                        onChange={(e) => setForm({ ...form, rowsCount: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>Seats Per Row</label>
                    <input
                        type="number"
                        min="1"
                        value={form.seatsPerRow}
                        onChange={(e) => setForm({ ...form, seatsPerRow: e.target.value })}
                        required
                    />
                </div>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                    {loading ? 'Creating...' : 'Create Hall & Generate Seats'}
                </button>
            </form>
        </div>
    );
}
