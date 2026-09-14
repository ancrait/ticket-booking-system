import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import { Layout } from './components/Layout.jsx';
import { PrivateRoute } from './components/PrivateRoute.jsx';
import { OrganizerRoute } from './components/OrganizerRoute.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import VerifyEmail from './pages/VerifyEmail.jsx';
import Home from './pages/Home.jsx';
import VenueSchedule from './pages/VenueSchedule.jsx';
import EventDetails from './pages/EventDetails.jsx';
import MyBookings from './pages/MyBookings.jsx';
import OrganizerDashboard from './pages/OrganizerDashboard.jsx';
import OrganizerEvents from './pages/OrganizerEvents.jsx';
import CreateEvent from './pages/CreateEvent.jsx';
import CreateVenue from './pages/CreateVenue.jsx';
import CreateHall from './pages/CreateHall.jsx';
import NotFound from './pages/NotFound.jsx';

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Layout />}>
                        <Route index element={<Home />} />
                        <Route path="venues/:venueId/schedule" element={<VenueSchedule />} />
                        <Route path="events/:id" element={<EventDetails />} />
                        <Route path="login" element={<Login />} />
                        <Route path="register" element={<Register />} />
                        <Route path="verify-email" element={<VerifyEmail />} />
                        <Route
                            path="my-bookings"
                            element={
                                <PrivateRoute>
                                    <MyBookings />
                                </PrivateRoute>
                            }
                        />
                        <Route
                            path="organizer"
                            element={
                                <OrganizerRoute>
                                    <OrganizerDashboard />
                                </OrganizerRoute>
                            }
                        />
                        <Route
                            path="organizer/events"
                            element={
                                <OrganizerRoute>
                                    <OrganizerEvents />
                                </OrganizerRoute>
                            }
                        />
                        <Route
                            path="organizer/events/new"
                            element={
                                <OrganizerRoute>
                                    <CreateEvent />
                                </OrganizerRoute>
                            }
                        />
                        <Route
                            path="organizer/venues/new"
                            element={
                                <OrganizerRoute>
                                    <CreateVenue />
                                </OrganizerRoute>
                            }
                        />
                        <Route
                            path="organizer/halls/new"
                            element={
                                <OrganizerRoute>
                                    <CreateHall />
                                </OrganizerRoute>
                            }
                        />
                        <Route path="*" element={<NotFound />} />
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;
