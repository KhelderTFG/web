import applyCaseMiddleware from 'axios-case-converter';
import axios from 'axios';

const client = applyCaseMiddleware(axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
}));

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('khelder_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRoute = error.config?.url?.includes('/auth/');
    if (!isAuthRoute &&
        (error.response?.status === 403 || error.response?.status === 401)) {
      localStorage.removeItem('khelder_token');
      localStorage.removeItem('khelder_caregiver');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default client;