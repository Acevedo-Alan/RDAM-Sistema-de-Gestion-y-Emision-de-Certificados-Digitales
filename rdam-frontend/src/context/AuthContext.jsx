import { createContext, useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export const AuthContext = createContext(null);

const STORAGE_KEY = 'accessToken';

const HOME_BY_ROLE = {
  admin: '/admin',
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

export function AuthProvider({ children }) {
  // ── Inicialización sincrónica: lee localStorage ANTES del primer render ──
  const [token, setToken] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved && !isTokenExpired(saved)) {
      window.__authToken = saved;
      return saved;
    }
    return null;
  });
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved && !isTokenExpired(saved)) return decodeToken(saved);
    return null;
  });
  const [isLoading, setIsLoading] = useState(false);
  const [sessionExpired, setSessionExpired] = useState(false);
  const navigate = useNavigate();

  // ── Side effects: limpieza de tokens expirados, logging ──
  useEffect(() => {
    console.log('[AUTH] Inicializando contexto...');
    console.log('[AUTH] URL actual:', window.location.href);

    const saved = localStorage.getItem(STORAGE_KEY);

    if (saved && isTokenExpired(saved)) {
      const decoded = decodeToken(saved);
      console.warn(
        '[AUTH] Token expirado, limpiando storage.',
        'Expiró en:', decoded?.exp ? new Date(decoded.exp * 1000).toISOString() : 'N/A',
        'Ahora:', new Date().toISOString()
      );
      localStorage.removeItem(STORAGE_KEY);
      window.__authToken = null;
      setToken(null);
      setUser(null);
      setSessionExpired(true);
    } else if (!saved) {
      const isPublicRoute = ['/login', '/register', '/verify'].includes(window.location.pathname);
      if (!isPublicRoute) {
        console.warn('[AUTH] Ruta protegida sin token, marcando sesión expirada.');
        setSessionExpired(true);
      }
    } else {
      console.log('[AUTH] Token válido. Rol:', user?.rol);
    }

    console.log('[AUTH] Inicialización completa.');
  }, []);

  const logout = useCallback(() => {
    console.log('[AUTH] Logout ejecutado.');
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
    console.log('[AUTH] Login exitoso. Rol:', decoded.rol);
    setToken(newToken);
    setUser(decoded);
    localStorage.setItem(STORAGE_KEY, newToken);
    window.__authToken = newToken;
    window.__authLogout = logout;
    navigate(HOME_BY_ROLE[decoded.rol] || '/login');
  }, [logout, navigate]);

  // Exponer logout al interceptor de axios
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
