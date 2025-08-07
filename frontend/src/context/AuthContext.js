import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

const AuthContext = createContext({ user: null, token: null, ready: true, login: () => {}, logout: () => {} });

function readStoredAuth() {
  try {
    const stored = localStorage.getItem('auth');
    if (stored) {
      const parsed = JSON.parse(stored);
      return { user: parsed.user || null, token: parsed.token || null };
    }
  } catch {}
  return { user: null, token: null };
}

export const AuthProvider = ({ children }) => {
  const initial = readStoredAuth();
  const [user, setUser] = useState(initial.user);
  const [token, setToken] = useState(initial.token);
  const [ready, setReady] = useState(true);

  useEffect(() => {
    // No-op since we read synchronously; keep hook to match SSR parity if needed
    setReady(true);
  }, []);

  const login = (nextToken, nextUser) => {
    setUser(nextUser || null);
    setToken(nextToken || null);
    localStorage.setItem('auth', JSON.stringify({ token: nextToken, user: nextUser }));
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('auth');
  };

  const value = useMemo(() => ({ user, token, ready, login, logout }), [user, token, ready]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);


