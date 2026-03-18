import { AppBar, Toolbar, IconButton, Typography, Box } from '@mui/material';
import { Menu as MenuIcon } from '@mui/icons-material';

export default function TopBar({ onOpen }) {
  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        bgcolor: '#1B1B1B',
        color: '#FFFFFF',
        boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
        height: 56,
        display: { xs: 'flex', md: 'none' },
      }}
    >
      <Toolbar sx={{ minHeight: 56 }}>
        <IconButton edge="start" onClick={onOpen} sx={{ mr: 2, color: '#FFFFFF' }}>
          <MenuIcon />
        </IconButton>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexGrow: 1 }}>
          <Box
            sx={{
              width: 32,
              height: 32,
              bgcolor: '#005EA2',
              borderRadius: '4px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Typography sx={{ color: '#FFFFFF', fontWeight: 700, fontSize: 18, lineHeight: 1 }}>
              R
            </Typography>
          </Box>
          <Typography sx={{ color: '#FFFFFF', fontWeight: 600, fontSize: 16 }}>RDAM</Typography>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
