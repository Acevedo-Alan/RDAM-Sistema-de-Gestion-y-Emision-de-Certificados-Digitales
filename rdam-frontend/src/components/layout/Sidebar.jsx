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
                borderRadius: '4px',
                mb: '2px',
                fontSize: 14,
                transition: 'all 0.2s',
                bgcolor: isActive ? '#005EA2' : 'transparent',
                color: isActive ? '#FFFFFF' : '#A9AEB1',
                '&:hover': isActive
                  ? {}
                  : { bgcolor: '#454545', color: '#FFFFFF' },
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

function SidebarContent({ onNavigate }) {
  const { user, logout } = useAuth();
  const items = menuByRole[user?.rol] || [];
  const emailStr = user?.email || user?.sub || '';
  const initial = emailStr.charAt(0).toUpperCase();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: '#1B1B1B' }}>
      {/* Logo header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          px: 3,
          py: 3,
          borderBottom: '1px solid #454545',
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
        <Typography sx={{ color: '#FFFFFF', fontWeight: 600, fontSize: 16 }}>RDAM</Typography>
      </Box>

      {/* Nav */}
      <NavItems items={items} onNavigate={onNavigate} />

      {/* User footer */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          px: 2,
          py: 1.5,
          borderTop: '1px solid #454545',
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
            color: '#A9AEB1',
            fontSize: 13,
          }}
        >
          {emailStr}
        </Typography>
        <IconButton size="small" onClick={logout} title="Cerrar sesion" sx={{ color: '#A9AEB1' }}>
          <LogoutIcon fontSize="small" />
        </IconButton>
      </Box>

      {/* Version tag */}
      <Box sx={{ px: 2, py: 1.5, borderTop: '1px solid #454545', textAlign: 'center' }}>
        <Typography sx={{ color: '#71767A', fontSize: 11, fontWeight: 500 }}>
          v1.0
        </Typography>
      </Box>
    </Box>
  );
}

export default function Sidebar() {
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
        backgroundColor: '#1B1B1B',
      }}
    >
      <SidebarContent />
    </Box>
  );
}

export { SidebarContent, menuByRole };
