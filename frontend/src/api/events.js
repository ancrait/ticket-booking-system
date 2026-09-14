import { apiClient } from './client.js';

export const eventsApi = {
    getEvents: (page = 0, size = 10, sortBy = 'id') =>
        apiClient.get(`/api/events?page=${page}&size=${size}&sortBy=${sortBy}`),
    searchEvents: (city, date, page = 0, size = 10, sortBy = 'id') => {
        let url = `/api/events/sortedBy?page=${page}&size=${size}&sortBy=${sortBy}`;
        if (city) url += `&city=${encodeURIComponent(city)}`;
        if (date) url += `&date=${date}`;
        return apiClient.get(url);
    },
    getEvent: (id) => apiClient.get(`/api/events/${id}`),
    getEventSeats: (eventId, page = 0, size = 100, sortBy = 'id') =>
        apiClient.get(`/api/events/${eventId}/seats?page=${page}&size=${size}&sortBy=${sortBy}`),
    getAvailableSeats: (eventId, page = 0, size = 100, sortBy = 'id') =>
        apiClient.get(`/api/events/${eventId}/seats/available?page=${page}&size=${size}&sortBy=${sortBy}`),
    createEvent: (data) => apiClient.post('/api/events', data),
    publishEvent: (id) => apiClient.post(`/api/events/${id}/publish`),
    cancelEvent: (id) => apiClient.post(`/api/events/${id}/cancel`),
    getMyEvents: () => apiClient.get('/api/events/organizer/my'),
    getEventsByVenue: (venueId, page = 0, size = 50, sortBy = 'startsAt') =>
        apiClient.get(`/api/events/by-venue/${venueId}?page=${page}&size=${size}&sortBy=${sortBy}`),
};
