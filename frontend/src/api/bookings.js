import { apiClient } from './client.js';

export const bookingsApi = {
    createBooking: (data) => apiClient.post('/api/bookings', data),
    getMyBookings: (page = 0, size = 10, sortBy = 'createdAt') =>
        apiClient.get(`/api/bookings/my?page=${page}&size=${size}&sortBy=${sortBy}`),
    getBooking: (id) => apiClient.get(`/api/bookings/${id}`),
    cancelBooking: (id) => apiClient.patch(`/api/bookings/${id}/cancel`),
};
