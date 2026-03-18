import { Chip } from '@mui/material';
import estadoColors from '../../utils/estadoColors';

export default function StatusBadge({ estado }) {
  const config = estadoColors[estado] || { label: estado, bg: '#DCDEE0', text: '#71767A' };

  return (
    <Chip
      label={config.label}
      size="small"
      sx={{
        bgcolor: config.bg,
        color: config.text,
        fontWeight: 600,
        fontSize: 11,
        textTransform: 'uppercase',
        letterSpacing: '0.3px',
        borderRadius: '12px',
        px: 1.5,
        py: 0.5,
        whiteSpace: 'nowrap',
      }}
    />
  );
}
