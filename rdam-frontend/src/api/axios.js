import axios from 'axios';

const api = axios.create({
  baseURL: '',
});

// ── Request interceptor ──
api.interceptors.request.use((config) => {
  const token = window.__authToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    // No enviar "Bearer null" ni "Bearer undefined"
    delete config.headers.Authorization;
    console.warn('[AXIOS] No hay token en memoria, request sin Authorization:', config.url);
  }
  return config;
});

// ── Response interceptor ──
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.error(
        '╔══════════════════════════════════════════════════════════╗\n' +
        '║  [AXIOS] ERROR 401 — SESIÓN RECHAZADA POR EL BACKEND   ║\n' +
        '╚══════════════════════════════════════════════════════════╝\n' +
        '  Endpoint: ', error.config?.url, '\n' +
        '  Método:   ', error.config?.method?.toUpperCase(), '\n' +
        '  Token enviado: ', error.config?.headers?.Authorization ? 'Sí' : 'NO', '\n' +
        '  Response data: ', error.response?.data
      );

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
