import { Typography, Box, Divider } from '@mui/material';

export default function PageHeader({ title, subtitle, children }) {
  return (
    <Box sx={{ mb: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 3 }}>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h4" sx={{ fontSize: 22, fontWeight: 700, color: '#1B1B1B' }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body2" sx={{ color: '#71767A', mt: 0.5 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {children && <Box sx={{ flexShrink: 0 }}>{children}</Box>}
      </Box>
      <Divider sx={{ mt: 2, mb: 3, borderColor: '#DCDEE0' }} />
    </Box>
  );
}
