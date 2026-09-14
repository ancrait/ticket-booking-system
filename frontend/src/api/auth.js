import { apiClient } from './client.js';

export const authApi = {
    register: (data) => apiClient.post('/api/auth/register', data),
    login: (data) => apiClient.post('/api/auth/login', data),
    refresh: (refreshToken) => apiClient.post('/api/auth/refresh', { refreshToken }),
    verifyEmail: (token) => apiClient.get(`/api/auth/verify-email?token=${token}`),
    resendVerification: (email) => apiClient.post('/api/auth/resend-verification', { email }),
    me: () => apiClient.get('/api/auth/me'),
};
