import { Box, Card, CardContent, Typography, Grid } from '@mui/material';
import {
  People as PeopleIcon,
  Category as CategoryIcon,
  Description as DescriptionIcon,
  TrendingUp as TrendingUpIcon,
} from '@mui/icons-material';
import PageHeader from '../../components/common/PageHeader';

const kpis = [
  {
    label: 'Usuarios activos',
    value: 128,
    delta: '+12 este mes',
    icon: <PeopleIcon />,
    color: '#005EA2',
    bg: '#E8F0FE',
  },
  {
    label: 'Catálogos totales',
    value: 14,
    delta: '2 nuevos',
    icon: <CategoryIcon />,
    color: '#00A91C',
    bg: '#E6F4EA',
  },
  {
    label: 'Solicitudes del mes',
    value: 342,
    delta: '+8% vs. anterior',
    icon: <DescriptionIcon />,
    color: '#FFBE2E',
    bg: '#FFF8E1',
  },
  {
    label: 'Tasa de aprobación',
    value: '94%',
    delta: '+2pp',
    icon: <TrendingUpIcon />,
    color: '#D54309',
    bg: '#FDECEA',
  },
];

export default function DashboardPage() {
  return (
    <>
      <PageHeader
        title="Dashboard"
        subtitle="Resumen general del sistema RDAM"
      />

      <Grid container spacing={3}>
        {kpis.map((kpi) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={kpi.label}>
            <Card
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              }}
            >
              <CardContent sx={{ p: 3, '&:last-child': { pb: 3 } }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: 13, mb: 1 }}>
                      {kpi.label}
                    </Typography>
                    <Typography variant="h4" sx={{ fontWeight: 700, color: 'text.primary', fontSize: 28 }}>
                      {kpi.value}
                    </Typography>
                    <Typography variant="caption" sx={{ color: 'text.secondary', mt: 0.5, display: 'block' }}>
                      {kpi.delta}
                    </Typography>
                  </Box>
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      bgcolor: kpi.bg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: kpi.color,
                    }}
                  >
                    {kpi.icon}
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3} sx={{ mt: 1 }}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              height: 320,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Box sx={{ textAlign: 'center' }}>
              <TrendingUpIcon sx={{ fontSize: 48, color: 'divider', mb: 1 }} />
              <Typography color="text.secondary">
                Gráfico de solicitudes por mes (próximamente)
              </Typography>
            </Box>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              height: 320,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Box sx={{ textAlign: 'center' }}>
              <PeopleIcon sx={{ fontSize: 48, color: 'divider', mb: 1 }} />
              <Typography color="text.secondary">
                Actividad reciente (próximamente)
              </Typography>
            </Box>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
