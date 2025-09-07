import axios from 'axios';
import { incrementLoading, decrementLoading } from './loadingTracker';

// Build a robust API base URL:
// - If REACT_APP_API_URL is provided, accept either root (http://host) or already '/api'
// - Otherwise default to localhost root
const apiRoot = process.env.REACT_APP_API_URL || 'http://localhost:8080';
const baseUrl = apiRoot.endsWith('/api') ? apiRoot : `${apiRoot}/api`;

const client = axios.create({ baseURL: baseUrl });

// In-memory refresh flow state to de-duplicate concurrent 401s
let isRefreshing = false;
let refreshPromise = null;
const refreshWaitQueue = [];

async function runQueuedCallbacks(error, token) {
  while (refreshWaitQueue.length) {
    const { resolve, reject } = refreshWaitQueue.shift();
    if (error) reject(error);
    else resolve(token);
  }
}

client.interceptors.request.use((config) => {
  incrementLoading();
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
  (res) => {
    decrementLoading();
    return res;
  },
  async (err) => {
    decrementLoading();
    const status = err?.response?.status;
    const original = err?.config;
    if (status !== 401 || original?._retry) {
      return Promise.reject(err);
    }

    // Attempt refresh token rotation
    original._retry = true;
    try {
      const stored = localStorage.getItem('auth');
      const auth = stored ? JSON.parse(stored) : null;
      const refreshToken = auth?.refreshToken;
      if (!refreshToken) throw err;

      if (!isRefreshing) {
        isRefreshing = true;
        refreshPromise = (async () => {
          try {
            const resp = await axios.post(`${baseUrl}/auth/refresh`, { refreshToken }, { headers: { 'Content-Type': 'application/json' } });
            const data = resp.data;
            const newAuth = { token: data.token, refreshToken: data.refreshToken, user: { username: data.username, email: data.email } };
            localStorage.setItem('auth', JSON.stringify(newAuth));
            await runQueuedCallbacks(null, data.token);
            return data.token;
          } catch (e) {
            await runQueuedCallbacks(e, null);
            throw e;
          } finally {
            isRefreshing = false;
            refreshPromise = null;
          }
        })();
      }

      const newToken = await refreshPromise;
      // Replay original request with new token
      original.headers = original.headers || {};
      original.headers.Authorization = `Bearer ${newToken}`;
      return client.request(original);
    } catch (refreshErr) {
      // Refresh failed -> clear auth and redirect
      try { localStorage.removeItem('auth'); } catch {}
      if (window.location.pathname !== '/member-login') {
        window.location.replace('/member-login');
      }
      return Promise.reject(refreshErr);
    }
  }
);

export default client;


