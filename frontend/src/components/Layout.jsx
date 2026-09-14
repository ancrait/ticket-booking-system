import { Outlet, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export function Layout() {
    const { user, isAuthenticated, logout } = useAuth();

    return (
        <div id="root">
            <nav className="navbar">
                <div className="container">
                    <Link to="/" className="navbar-brand">
                        TIQ Tickets
                    </Link>
                    <div className="navbar-links">
                        <Link to="/">Зали</Link>
                        {isAuthenticated ? (
                            <>
                                {(user?.role === 'ORGANIZER' || user?.role === 'ADMIN') && (
                                    <Link to="/organizer">Organizer</Link>
                                )}
                                <Link to="/my-bookings">My Bookings</Link>
                                <span className="text-muted">{user?.email}</span>
                                <button onClick={logout}>Logout</button>
                            </>
                        ) : (
                            <>
                                <Link to="/login">Login</Link>
                                <Link to="/register">Register</Link>
                            </>
                        )}
                    </div>
                </div>
            </nav>
            <main className="container">
                <Outlet />
            </main>
        </div>
    );
}
