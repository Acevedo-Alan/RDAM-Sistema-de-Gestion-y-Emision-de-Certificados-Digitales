import { NavLink } from 'react-router-dom';
import { Box, Typography, Avatar, IconButton } from '@mui/material';
import {
  ListAlt as ListAltIcon,
  AddCircleOutline as AddCircleOutlineIcon,
  Inbox as InboxIcon,
  People as PeopleIcon,
  Category as CategoryIcon,
  Assessment as AssessmentIcon,
  History as HistoryIcon,
  Logout as LogoutIcon,
  Dashboard as DashboardIcon,
  DarkMode,
  LightMode,
} from '@mui/icons-material';
import { useAuth } from '../../hooks/useAuth';

const menuByRole = {
  ciudadano: [
    { label: 'Mis Solicitudes', path: '/ciudadano/solicitudes', icon: <ListAltIcon /> },
    { label: 'Nueva Solicitud', path: '/ciudadano/nueva', icon: <AddCircleOutlineIcon /> },
  ],
  interno: [
    { label: 'Bandeja', path: '/interno/bandeja', icon: <InboxIcon /> },
    { label: 'Historial', path: '/interno/historial', icon: <HistoryIcon /> },
  ],
  admin: [
    { label: 'Dashboard', path: '/admin', icon: <DashboardIcon /> },
    { label: 'Usuarios', path: '/admin/usuarios', icon: <PeopleIcon /> },
    { label: 'Catalogos', path: '/admin/catalogos', icon: <CategoryIcon /> },
    { label: 'Reportes', path: '/admin/reportes', icon: <AssessmentIcon /> },
  ],
};

function NavItems({ items, onNavigate }) {
  return (
    <nav style={{ flex: 1, marginTop: 8, padding: '0 8px' }}>
      {items.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          end={item.path === '/admin'}
          onClick={onNavigate}
          style={{ textDecoration: 'none' }}
        >
          {({ isActive }) => (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                px: '16px',
                py: '8px',
                borderRadius: '8px',
                mb: '2px',
                fontSize: 14,
                transition: 'all 0.2s',
                borderLeft: isActive ? '3px solid #005EA2' : '3px solid transparent',
                bgcolor: isActive ? '#EEF4FF' : 'transparent',
                color: isActive ? '#005EA2' : '#71767A',
                '&:hover': {
                  bgcolor: isActive ? '#EEF4FF' : '#F0F0F0',
                },
              }}
            >
              <Box sx={{ display: 'flex', fontSize: 18, opacity: 0.8, color: 'inherit' }}>{item.icon}</Box>
              <span>{item.label}</span>
            </Box>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

function SidebarContent({ onNavigate, toggleMode, mode }) {
  const { user, logout } = useAuth();
  const items = menuByRole[user?.rol] || [];
  const emailStr = user?.email || user?.sub || '';
  const initial = emailStr.charAt(0).toUpperCase();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: 'background.paper' }}>
      {/* Logo header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          px: 3,
          py: 3,
          borderBottom: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Box
          sx={{
            width: 32,
            height: 32,
            bgcolor: '#005EA2',
            borderRadius: '4px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <Typography sx={{ color: '#FFFFFF', fontWeight: 700, fontSize: 18, lineHeight: 1 }}>
            R
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', flexDirection: 'column' }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, color: 'text.primary', lineHeight: 1.2 }}>
            RDAM
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Sistema de Certificados
          </Typography>
        </Box>
      </Box>

      {/* Nav */}
      <NavItems items={items} onNavigate={onNavigate} />

      {/* Dark mode toggle */}
      <Box sx={{ px: 1, pb: 1 }}>
        <Box
          onClick={toggleMode}
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            px: 2,
            py: 1,
            mx: 1,
            mb: 1,
            borderRadius: '999px',
            cursor: 'pointer',
            bgcolor: (theme) =>
              theme.palette.mode === 'light'
                ? 'rgba(0, 0, 0, 0.06)'
                : 'rgba(255, 255, 255, 0.08)',
            border: '1px solid',
            borderColor: (theme) =>
              theme.palette.mode === 'light'
                ? 'rgba(0, 0, 0, 0.12)'
                : 'rgba(255, 255, 255, 0.12)',
            color: 'text.secondary',
            transition: 'all 0.2s',
            '&:hover': {
              bgcolor: (theme) =>
                theme.palette.mode === 'light'
                  ? 'rgba(0, 0, 0, 0.10)'
                  : 'rgba(255, 255, 255, 0.14)',
              color: 'text.primary',
            },
          }}
        >
          {mode === 'dark'
            ? <LightMode sx={{ fontSize: 18 }} />
            : <DarkMode sx={{ fontSize: 18 }} />
          }
          <Typography variant="body2" sx={{ fontSize: 13, fontWeight: 500 }}>
            {mode === 'dark' ? 'Modo claro' : 'Modo oscuro'}
          </Typography>
        </Box>
      </Box>

      {/* User footer */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          px: 2,
          py: 1.5,
          borderTop: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Avatar sx={{ width: 32, height: 32, bgcolor: '#005EA2', color: '#FFFFFF', fontSize: 14 }}>
          {initial}
        </Avatar>
        <Typography
          sx={{
            flex: 1,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            color: 'text.secondary',
            fontSize: 13,
          }}
        >
          {emailStr}
        </Typography>
        <IconButton size="small" onClick={logout} title="Cerrar sesion" sx={{ color: 'text.secondary' }}>
          <LogoutIcon fontSize="small" />
        </IconButton>
      </Box>

      {/* Version tag */}
      <Box sx={{ px: 2, py: 1.5, borderTop: '1px solid', borderColor: 'divider', textAlign: 'center' }}>
        <Typography sx={{ color: 'text.secondary', fontSize: 11, fontWeight: 500 }}>
          v1.0
        </Typography>
      </Box>
    </Box>
  );
}

export default function Sidebar({ toggleMode, mode }) {
  return (
    <Box
      component="aside"
      sx={{
        display: { xs: 'none', md: 'flex' },
        flexDirection: 'column',
        position: 'fixed',
        left: 0,
        top: 0,
        height: '100vh',
        width: 240,
        zIndex: 100,
        bgcolor: 'background.paper',
        borderRight: '1px solid',
        borderColor: 'divider',
      }}
    >
      <SidebarContent toggleMode={toggleMode} mode={mode} />
    </Box>
  );
}

export { SidebarContent, menuByRole };
