import axios from 'axios';

const api = axios.create({
  baseURL: '',
});

api.interceptors.request.use((config) => {
  const token = window.__authToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      if (window.__authLogout) {
        window.__authLogout();
      } else {
        localStorage.removeItem('accessToken');
        window.__authToken = null;
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;