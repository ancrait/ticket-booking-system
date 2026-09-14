import { Link } from 'react-router-dom';

export default function NotFound() {
    return (
        <div className="card" style={{ textAlign: 'center', maxWidth: 500, margin: '0 auto' }}>
            <h2>404 - Page Not Found</h2>
            <p className="text-muted">The page you are looking for does not exist.</p>
            <Link to="/events" className="btn btn-primary mt-4">
                Back to Events
            </Link>
        </div>
    );
}
