import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { venuesApi } from '../api/venues.js';

const CITIES = ['Київ', 'Львів', 'Одеса', 'Харків', 'Дніпро', 'Черкаси'];

export default function Home() {
    const [selectedCity, setSelectedCity] = useState('');
    const [venues, setVenues] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (selectedCity) {
            loadVenues(selectedCity);
        } else {
            loadAllVenues();
        }
    }, [selectedCity]);

    const loadVenues = async (city) => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await venuesApi.getVenues(0, 100, 'name', city);
            setVenues(data.content || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Не вдалося завантажити зали');
        } finally {
            setLoading(false);
        }
    };

    const loadAllVenues = async () => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await venuesApi.getVenues(0, 100, 'name');
            setVenues(data.content || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Не вдалося завантажити зали');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <div className="city-bar">
                <div className="container">
                    <label className="city-label">Місто:</label>
                    <select
                        className="city-select"
                        value={selectedCity}
                        onChange={(e) => setSelectedCity(e.target.value)}
                    >
                        <option value="">Всі міста</option>
                        {CITIES.map((city) => (
                            <option key={city} value={city}>
                                {city}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="container">
                {loading && <div className="loading">Завантаження...</div>}
                {error && <div className="error">{error}</div>}

                <div className="venues-list">
                    {venues.map((venue) => (
                        <div key={venue.id} className="venue-card">
                            <div className="venue-image">
                                <img
                                    src={`https://placehold.co/300x200/1a1a1a/ffffff?text=${encodeURIComponent(venue.name)}`}
                                    alt={venue.name}
                                    onError={(e) => {
                                        e.target.src = 'https://placehold.co/300x200/1a1a1a/ffffff?text=Cinema';
                                    }}
                                />
                            </div>
                            <div className="venue-info">
                                <div className="venue-header">
                                    <h2 className="venue-name">{venue.name}</h2>
                                    <span className="city-badge">{venue.city}</span>
                                </div>
                                <p className="venue-address">{venue.address}</p>
                                <Link
                                    to={`/venues/${venue.id}/schedule`}
                                    className="btn btn-multiplex"
                                >
                                    Дивитись розклад
                                </Link>
                            </div>
                        </div>
                    ))}
                </div>

                {venues.length === 0 && !loading && !error && (
                    <p className="text-muted" style={{ textAlign: 'center', padding: '48px 0' }}>
                        Зали не знайдено.
                    </p>
                )}
            </div>
        </div>
    );
}
