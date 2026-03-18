import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box } from '@mui/material';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import MobileDrawer from './MobileDrawer';

export default function AppLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#F9F9F9' }}>
      <Sidebar />
      <TopBar onOpen={() => setDrawerOpen(true)} />
      <MobileDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          ml: { xs: 0, md: '240px' },
          pt: { xs: '56px', md: 0 },
          minHeight: '100vh',
          bgcolor: '#F9F9F9',
        }}
      >
        <Box sx={{ p: { xs: 2, md: '32px 40px' } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
