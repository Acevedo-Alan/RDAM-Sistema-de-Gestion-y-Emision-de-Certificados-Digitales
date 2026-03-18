import api from '../axios';

export const loginApi = (data) => api.post('/auth/login', data);

export const verifyOtpApi = (data) => api.post('/auth/verify', data);

export const registerApi = (data) => api.post('/auth/register', data);
