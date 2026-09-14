import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export function OrganizerRoute({ children }) {
    const { user, loading } = useAuth();

    if (loading) {
        return <div className="loading">Loading...</div>;
    }

    const role = user?.role;
    if (role !== 'ORGANIZER' && role !== 'ADMIN') {
        return <Navigate to="/events" replace />;
    }

    return children;
}
