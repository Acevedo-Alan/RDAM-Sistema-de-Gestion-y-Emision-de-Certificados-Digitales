import { Box, Card, CardContent, Typography, Grid, Button } from '@mui/material';
import {
  Assessment as AssessmentIcon,
  PieChart as PieChartIcon,
  Timeline as TimelineIcon,
  Download as DownloadIcon,
  BarChart as BarChartIcon,
} from '@mui/icons-material';
import PageHeader from '../../components/common/PageHeader';

const reportes = [
  {
    nombre: 'Solicitudes por estado',
    descripcion: 'Distribución de solicitudes según su estado actual.',
    icon: <PieChartIcon />,
    color: '#005EA2',
    bg: '#E8F0FE',
  },
  {
    nombre: 'Solicitudes por mes',
    descripcion: 'Evolución mensual de las solicitudes recibidas.',
    icon: <BarChartIcon />,
    color: '#00A91C',
    bg: '#E6F4EA',
  },
  {
    nombre: 'Tiempos de resolución',
    descripcion: 'Tiempo promedio de procesamiento por tipo de trámite.',
    icon: <TimelineIcon />,
    color: '#FFBE2E',
    bg: '#FFF8E1',
  },
  {
    nombre: 'Actividad de usuarios',
    descripcion: 'Usuarios más activos y patrones de uso del sistema.',
    icon: <AssessmentIcon />,
    color: '#D54309',
    bg: '#FDECEA',
  },
];

export default function ReportesPage() {
  return (
    <>
      <PageHeader title="Reportes y estadísticas" subtitle="Visualiza métricas y genera reportes del sistema">
        <Button variant="contained" startIcon={<DownloadIcon />}>
          Exportar datos
        </Button>
      </PageHeader>

      <Grid container spacing={3}>
        {reportes.map((rep) => (
          <Grid size={{ xs: 12, sm: 6 }} key={rep.nombre}>
            <Card
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  borderColor: 'primary.main',
                  boxShadow: '0 4px 12px rgba(0,94,162,0.15)',
                },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      bgcolor: rep.bg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: rep.color,
                    }}
                  >
                    {rep.icon}
                  </Box>
                  <Typography sx={{ fontWeight: 600, fontSize: 16, color: 'text.primary' }}>
                    {rep.nombre}
                  </Typography>
                </Box>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
                  {rep.descripcion}
                </Typography>
                <Box
                  sx={{
                    height: 120,
                    borderRadius: 2,
                    bgcolor: '#F9F9F9',
                    border: '1px dashed',
                    borderColor: 'divider',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    Gráfico próximamente
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
