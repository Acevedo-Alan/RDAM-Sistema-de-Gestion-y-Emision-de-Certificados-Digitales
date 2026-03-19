import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CircularProgress, Typography } from '@mui/material';
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

import DashboardPage from '../pages/admin/DashboardPage';
import UsuariosPage from '../pages/admin/UsuariosPage';
import CatalogosPage from '../pages/admin/CatalogosPage';
import ReportesPage from '../pages/admin/ReportesPage';
import DetalleSolicitudAdminPage from '../pages/admin/DetalleSolicitudAdminPage';
import VerificarCertificadoPage from '../pages/public/VerificarCertificadoPage';

function AuthSpinner() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100vh', gap: 2 }}>
      <CircularProgress />
      <Typography variant="body2" color="text.secondary">
        Cargando sesión...
      </Typography>
    </Box>
  );
}

// IMPORTANTE: NO evalúa el token mientras isLoading sea true.
// Esto evita el "parpadeo" a /login cuando se vuelve de PlusPagos.
function ProtectedRoute({ children }) {
  const { token, isLoading } = useAuth();

  if (isLoading) {
    console.log('[ROUTER] ProtectedRoute: isLoading=true, mostrando spinner (NO se evalúa token aún)');
    return <AuthSpinner />;
  }

  console.log('[ROUTER] ProtectedRoute: isLoading=false, token:', !!token);

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

{/*
  ╔══════════════════════════════════════════════════════════════════╗
  ║  VERIFICAR URL DE REDIRECCIÓN DE PLUSPAGOS                      ║
  ║                                                                  ║
  ║  La URL de retorno configurada en PlusPagos DEBE coincidir       ║
  ║  EXACTAMENTE con el origen donde corre este frontend:            ║
  ║                                                                  ║
  ║  - Si tu frontend corre en http://localhost:5173                 ║
  ║    la URL de retorno NO puede ser http://127.0.0.1:5173          ║
  ║    porque localStorage es POR ORIGEN (protocolo + host + puerto) ║
  ║                                                                  ║
  ║  - Tampoco puede ser http://localhost:3000 si el frontend        ║
  ║    corre en otro puerto.                                         ║
  ║                                                                  ║
  ║  localStorage("localhost:5173") !== localStorage("127.0.0.1:5173")║
  ║                                                                  ║
  ║  Abre DevTools > Application > Storage y compara los orígenes.   ║
  ╚══════════════════════════════════════════════════════════════════╝
*/}

export default function AppRouter() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/verify" element={<OtpPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verificar/:token" element={<VerificarCertificadoPage />} />

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
        <Route path="/admin" element={<RoleRoute role="admin"><DashboardPage /></RoleRoute>} />
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
