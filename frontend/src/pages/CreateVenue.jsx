import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { venuesApi } from '../api/venues.js';

export default function CreateVenue() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ name: '', city: '', address: '' });
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            await venuesApi.createVenue(form);
            navigate('/organizer');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create venue');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card" style={{ maxWidth: 500, margin: '0 auto' }}>
            <h2>Create Venue</h2>
            {error && <div className="error">{error}</div>}
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Name</label>
                    <input
                        type="text"
                        value={form.name}
                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>City</label>
                    <input
                        type="text"
                        value={form.city}
                        onChange={(e) => setForm({ ...form, city: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label>Address</label>
                    <input
                        type="text"
                        value={form.address}
                        onChange={(e) => setForm({ ...form, address: e.target.value })}
                        required
                    />
                </div>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                    {loading ? 'Creating...' : 'Create Venue'}
                </button>
            </form>
        </div>
    );
}
