import { apiClient } from './client.js';

export const venuesApi = {
    getVenues: (page = 0, size = 100, sortBy = 'id', city) => {
        let url = `/api/venues?page=${page}&size=${size}&sortBy=${sortBy}`;
        if (city) url += `&city=${encodeURIComponent(city)}`;
        return apiClient.get(url);
    },
    getVenue: (id) => apiClient.get(`/api/venues/${id}`),
    createVenue: (data) => apiClient.post('/api/venues', data),
    updateVenue: (id, data) => apiClient.put(`/api/venues/${id}`, data),
    deleteVenue: (id) => apiClient.delete(`/api/venues/${id}`),
    getHalls: (venueId) => apiClient.get(`/api/venues/${venueId}/halls`),
    createHall: (venueId, data) => apiClient.post(`/api/venues/${venueId}/halls`, data),
};

export const hallsApi = {
    getHall: (id) => apiClient.get(`/api/halls/${id}`),
    updateHall: (id, data) => apiClient.put(`/api/halls/${id}`, data),
    deleteHall: (id) => apiClient.delete(`/api/halls/${id}`),
    generateSeats: (hallId) => apiClient.post(`/api/halls/${hallId}/seats`),
    getSeats: (hallId, page = 0, size = 100, sortBy = 'id') =>
        apiClient.get(`/api/halls/${hallId}/seats?page=${page}&size=${size}&sortBy=${sortBy}`),
};
