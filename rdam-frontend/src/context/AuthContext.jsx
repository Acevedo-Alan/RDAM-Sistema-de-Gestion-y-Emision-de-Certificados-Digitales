import { createContext, useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export const AuthContext = createContext(null);

const STORAGE_KEY = 'accessToken';

const HOME_BY_ROLE = {
  admin: '/admin/usuarios',
  interno: '/interno/bandeja',
  ciudadano: '/ciudadano/solicitudes',
};

function decodeToken(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return { email: payload.email, rol: payload.rol, sub: payload.sub, exp: payload.exp };
  } catch {
    return null;
  }
}

function isTokenExpired(token) {
  const decoded = decodeToken(token);
  if (!decoded?.exp) return true;
  return decoded.exp * 1000 < Date.now();
}

function loadTokenFromStorage() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (!saved) return null;
  if (isTokenExpired(saved)) {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
  return saved;
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => {
    const saved = loadTokenFromStorage();
    if (saved) window.__authToken = saved;
    return saved;
  });
  const [user, setUser] = useState(() => {
    const saved = loadTokenFromStorage();
    return saved ? decodeToken(saved) : null;
  });
  const [isLoading, setIsLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);
  const navigate = useNavigate();

  // Mark initialization complete after first render
  useEffect(() => {
    if (!token && window.location.pathname !== '/login'
        && window.location.pathname !== '/register'
        && window.location.pathname !== '/verify') {
      setSessionExpired(true);
    }
    setIsLoading(false);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(STORAGE_KEY);
    window.__authToken = null;
    window.__authLogout = null;
    navigate('/login');
  }, [navigate]);

  const login = useCallback((newToken) => {
    const decoded = decodeToken(newToken);
    if (!decoded) return;
    setToken(newToken);
    setUser(decoded);
    localStorage.setItem(STORAGE_KEY, newToken);
    window.__authToken = newToken;
    window.__authLogout = logout;
    navigate(HOME_BY_ROLE[decoded.rol] || '/login');
  }, [logout, navigate]);

  // Expose logout to axios interceptor
  useEffect(() => {
    window.__authLogout = logout;
    return () => { window.__authLogout = null; };
  }, [logout]);

  const clearSessionExpired = useCallback(() => setSessionExpired(false), []);

  return (
    <AuthContext.Provider value={{ token, user, login, logout, isLoading, sessionExpired, clearSessionExpired, HOME_BY_ROLE }}>
      {children}
    </AuthContext.Provider>
  );
}
