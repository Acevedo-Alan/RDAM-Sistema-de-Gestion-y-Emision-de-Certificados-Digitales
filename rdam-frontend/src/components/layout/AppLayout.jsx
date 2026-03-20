import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box } from '@mui/material';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import MobileDrawer from './MobileDrawer';

export default function AppLayout({ toggleMode, mode }) {
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Sidebar toggleMode={toggleMode} mode={mode} />
      <TopBar onOpen={() => setDrawerOpen(true)} />
      <MobileDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} toggleMode={toggleMode} mode={mode} />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          ml: { xs: 0, md: '240px' },
          pt: { xs: '56px', md: 0 },
          minHeight: '100vh',
          bgcolor: 'background.default',
        }}
      >
        <Box sx={{ p: { xs: 2, md: '32px 40px' } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
