import { Drawer } from '@mui/material';
import { SidebarContent } from './Sidebar';

export default function MobileDrawer({ open, onClose, toggleMode, mode }) {
  return (
    <Drawer
      variant="temporary"
      anchor="left"
      open={open}
      onClose={onClose}
      sx={{
        display: { xs: 'block', md: 'none' },
        '& .MuiDrawer-paper': { width: 240, boxSizing: 'border-box', bgcolor: 'background.paper' },
      }}
    >
      <SidebarContent onNavigate={onClose} toggleMode={toggleMode} mode={mode} />
    </Drawer>
  );
}
