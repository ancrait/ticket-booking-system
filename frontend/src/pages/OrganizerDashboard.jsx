import { Link } from 'react-router-dom';

export default function OrganizerDashboard() {
    return (
        <div>
            <h2>Organizer Dashboard</h2>
            <div className="grid grid-3">
                <Link to="/organizer/venues/new" className="card event-card" style={{ textDecoration: 'none', color: 'inherit' }}>
                    <h3>Create Venue</h3>
                    <p className="text-muted">Add a new venue with address and city</p>
                </Link>
                <Link to="/organizer/halls/new" className="card event-card" style={{ textDecoration: 'none', color: 'inherit' }}>
                    <h3>Create Hall</h3>
                    <p className="text-muted">Add a hall to a venue and generate seats</p>
                </Link>
                <Link to="/organizer/events/new" className="card event-card" style={{ textDecoration: 'none', color: 'inherit' }}>
                    <h3>Create Event</h3>
                    <p className="text-muted">Create and publish a new event</p>
                </Link>
                <Link to="/organizer/events" className="card event-card" style={{ textDecoration: 'none', color: 'inherit' }}>
                    <h3>My Events</h3>
                    <p className="text-muted">Manage your events</p>
                </Link>
            </div>
        </div>
    );
}
