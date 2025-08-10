import axios from 'axios';

// Build a robust API base URL:
// - If REACT_APP_API_URL is provided, accept either root (http://host) or already '/api'
// - Otherwise default to localhost root
const apiRoot = process.env.REACT_APP_API_URL || 'http://localhost:8080';
const baseUrl = apiRoot.endsWith('/api') ? apiRoot : `${apiRoot}/api`;

const client = axios.create({ baseURL: baseUrl });

client.interceptors.request.use((config) => {
  try {
    const stored = localStorage.getItem('auth');
    if (stored) {
      const { token } = JSON.parse(stored);
      if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
  } catch {}
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      // Redirect to login on unauthorized
      if (window.location.pathname !== '/member-login') {
        window.location.replace('/member-login');
      }
    }
    return Promise.reject(err);
  }
);

export default client;


