import { createContext, useContext, useState, useCallback } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('civicpulse_user');
    return stored ? JSON.parse(stored) : null;
  });

  const [token, setToken] = useState(() => localStorage.getItem('civicpulse_token') || null);

  const login = useCallback(async (email, password) => {
    const res = await api.post('/auth/login', { email, password });
    const { token: jwt, ...userInfo } = res.data.data;
    localStorage.setItem('civicpulse_token', jwt);
    localStorage.setItem('civicpulse_user', JSON.stringify(userInfo));
    setToken(jwt);
    setUser(userInfo);
    return userInfo;
  }, []);

  const register = useCallback(async (payload) => {
    const res = await api.post('/auth/register', payload);
    return res.data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('civicpulse_token');
    localStorage.removeItem('civicpulse_user');
    setToken(null);
    setUser(null);
  }, []);

  const hasRole = useCallback((role) => {
    return user?.roles?.includes(`ROLE_${role.toUpperCase()}`);
  }, [user]);

  return (
    <AuthContext.Provider value={{ user, token, login, logout, register, hasRole, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
