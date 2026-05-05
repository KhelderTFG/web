import axios from 'axios';

const client = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor: añadir token JWT a todas las peticiones
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('khelder_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor: redirigir al login si el token ha expirado
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 403 || error.response?.status === 401) {
      localStorage.removeItem('khelder_token');
      localStorage.removeItem('khelder_caregiver');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;