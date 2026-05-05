import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import type { AuthResponse } from '../types';

export interface AuthContextType {
  caregiver:  AuthResponse | null;
  token:      string | null;
  isLoggedIn: boolean;
  login:      (response: AuthResponse) => void;
  logout:     () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [caregiver, setCaregiver] = useState<AuthResponse | null>(() => {
    const stored = localStorage.getItem('khelder_caregiver');
    return stored ? JSON.parse(stored) : null;
  });

  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('khelder_token')
  );

  const login = useCallback((response: AuthResponse) => {
    localStorage.setItem('khelder_token',     response.token);
    localStorage.setItem('khelder_caregiver', JSON.stringify(response));
    setToken(response.token);
    setCaregiver(response);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('khelder_token');
    localStorage.removeItem('khelder_caregiver');
    setToken(null);
    setCaregiver(null);
  }, []);

  return (
    <AuthContext.Provider value={{
      caregiver,
      token,
      isLoggedIn: !!token,
      login,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  );
};