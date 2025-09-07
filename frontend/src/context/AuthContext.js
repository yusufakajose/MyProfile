import React, { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';

const AuthContext = createContext({ user: null, token: null, refreshToken: null, ready: true, login: () => {}, logout: () => {}, setTokens: () => {} });

function readStoredAuth() {
  try {
    const stored = localStorage.getItem('auth');
    if (stored) {
      const parsed = JSON.parse(stored);
      return { user: parsed.user || null, token: parsed.token || null, refreshToken: parsed.refreshToken || null };
    }
  } catch {}
  return { user: null, token: null, refreshToken: null };
}

function getJwtExpSeconds(jwt) {
  if (!jwt) return 0;
  const parts = jwt.split('.');
  if (parts.length !== 3) return 0;
  try {
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' ? payload.exp : 0;
  } catch {
    return 0;
  }
}

export const AuthProvider = ({ children }) => {
  const initial = readStoredAuth();
  const [user, setUser] = useState(initial.user);
  const [token, setToken] = useState(initial.token);
  const [refreshToken, setRefreshToken] = useState(initial.refreshToken);
  const [ready, setReady] = useState(false);

  // Expose a unified setter so interceptors can update tokens
  const setTokens = useCallback((nextToken, nextRefreshToken, nextUser) => {
    const newUser = nextUser !== undefined ? nextUser : user;
    setUser(newUser || null);
    setToken(nextToken || null);
    setRefreshToken(nextRefreshToken || null);
    localStorage.setItem('auth', JSON.stringify({ token: nextToken || null, refreshToken: nextRefreshToken || null, user: newUser || null }));
  }, [user]);

  const login = useCallback((nextToken, nextUser, nextRefreshToken) => {
    setTokens(nextToken, nextRefreshToken, nextUser);
  }, [setTokens]);

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    setRefreshToken(null);
    localStorage.removeItem('auth');
  }, []);

  // Silent refresh shortly before token expiry, or if no access token but refresh exists
  useEffect(() => {
    let cancelled = false;
    async function maybeRefresh() {
      try {
        const nowSec = Math.floor(Date.now() / 1000);
        const exp = getJwtExpSeconds(token);
        const shouldRefresh = (!!refreshToken && (!token || exp - nowSec < 60));
        if (shouldRefresh) {
          const apiRoot = process.env.REACT_APP_API_URL || 'http://localhost:8080';
          const baseUrl = apiRoot.endsWith('/api') ? apiRoot : `${apiRoot}/api`;
          const res = await fetch(`${baseUrl}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
          });
          if (!res.ok) throw new Error('refresh failed');
          const data = await res.json();
          if (!cancelled) setTokens(data.token, data.refreshToken, { username: data.username, email: data.email });
        }
      } catch {
        if (!cancelled) logout();
      } finally {
        if (!cancelled) setReady(true);
      }
    }
    maybeRefresh();
    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const value = useMemo(() => ({ user, token, refreshToken, ready, login, logout, setTokens }), [user, token, refreshToken, ready, login, logout, setTokens]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);


