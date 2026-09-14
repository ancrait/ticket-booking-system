import { apiClient } from './client.js';

export const paymentsApi = {
    initiate: (bookingId) => apiClient.post(`/api/payments/${bookingId}/initiate`),
    getStatus: (bookingId) => apiClient.get(`/api/payments/${bookingId}/status`),
};
