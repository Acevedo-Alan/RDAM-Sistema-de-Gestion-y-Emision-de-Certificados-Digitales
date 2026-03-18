import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from '../hooks/useAuth';

import AppLayout from '../components/layout/AppLayout';

import LoginPage from '../pages/auth/LoginPage';
import OtpPage from '../pages/auth/OtpPage';
import RegisterPage from '../pages/auth/RegisterPage';

import SolicitudesPage from '../pages/ciudadano/SolicitudesPage';
import NuevaSolicitudPage from '../pages/ciudadano/NuevaSolicitudPage';
import DetalleSolicitudPage from '../pages/ciudadano/DetalleSolicitudPage';

import BandejaPage from '../pages/interno/BandejaPage';
import RevisionSolicitudPage from '../pages/interno/RevisionSolicitudPage';
import EmitirPage from '../pages/interno/EmitirPage';
import HistorialPage from '../pages/interno/HistorialPage';

import UsuariosPage from '../pages/admin/UsuariosPage';
import CatalogosPage from '../pages/admin/CatalogosPage';
import ReportesPage from '../pages/admin/ReportesPage';
import DetalleSolicitudAdminPage from '../pages/admin/DetalleSolicitudAdminPage';

function AuthSpinner() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <CircularProgress />
    </Box>
  );
}

function ProtectedRoute({ children }) {
  const { token, isLoading } = useAuth();
  if (isLoading) return <AuthSpinner />;
  if (!token) return <Navigate to="/login" replace />;
  return children;
}

function RoleRoute({ role, children }) {
  const { token, user, isLoading, HOME_BY_ROLE } = useAuth();
  if (isLoading) return <AuthSpinner />;
  if (!token) return <Navigate to="/login" replace />;
  if (user?.rol !== role) return <Navigate to={HOME_BY_ROLE[user?.rol] || '/login'} replace />;
  return children;
}

export default function AppRouter() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/verify" element={<OtpPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Protected routes with layout */}
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        {/* Ciudadano */}
        <Route path="/ciudadano/solicitudes" element={<RoleRoute role="ciudadano"><SolicitudesPage /></RoleRoute>} />
        <Route path="/ciudadano/nueva" element={<RoleRoute role="ciudadano"><NuevaSolicitudPage /></RoleRoute>} />
        <Route path="/ciudadano/solicitudes/:id" element={<RoleRoute role="ciudadano"><DetalleSolicitudPage /></RoleRoute>} />

        {/* Interno */}
        <Route path="/interno/bandeja" element={<RoleRoute role="interno"><BandejaPage /></RoleRoute>} />
        <Route path="/interno/solicitudes/:id" element={<RoleRoute role="interno"><RevisionSolicitudPage /></RoleRoute>} />
        <Route path="/interno/solicitudes/:id/emitir" element={<RoleRoute role="interno"><EmitirPage /></RoleRoute>} />
        <Route path="/interno/historial" element={<RoleRoute role="interno"><HistorialPage /></RoleRoute>} />

        {/* Admin */}
        <Route path="/admin/usuarios" element={<RoleRoute role="admin"><UsuariosPage /></RoleRoute>} />
        <Route path="/admin/catalogos" element={<RoleRoute role="admin"><CatalogosPage /></RoleRoute>} />
        <Route path="/admin/reportes" element={<RoleRoute role="admin"><ReportesPage /></RoleRoute>} />
        <Route path="/admin/solicitudes/:id" element={<RoleRoute role="admin"><DetalleSolicitudAdminPage /></RoleRoute>} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
