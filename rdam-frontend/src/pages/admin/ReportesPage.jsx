import { Box, Card, CardContent, Typography, Grid, Button, LinearProgress, Skeleton } from '@mui/material';
import {
  Download as DownloadIcon,
  Description as DescriptionIcon,
  People as PeopleIcon,
  Assessment as AssessmentIcon,
  LocationOn as LocationOnIcon,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import api from '../../api/axios';
import PageHeader from '../../components/common/PageHeader';

const handleDescargar = async (endpoint, filename) => {
  const token = window.__authToken;
  const response = await fetch(endpoint, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) return;
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

const hoy = dayjs().format('YYYY-MM-DD');

export default function ReportesPage() {
  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard-admin'],
    queryFn: () =>
      api.get('/api/dashboard/admin').then((r) => r.data).catch(() => null),
    refetchInterval: 60000,
  });

  const solicitudesPorCirc = dashboard?.solicitudesPorCircunscripcion ?? {};
  const totalCirc = Object.values(solicitudesPorCirc).reduce((a, b) => a + b, 0) || 1;

  return (
    <>
      <PageHeader
        title="Reportes y estadísticas"
        subtitle="Visualizá métricas y generá reportes del sistema"
      >
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            onClick={() =>
              handleDescargar('/api/admin/reportes/solicitudes.csv', `solicitudes-${hoy}.csv`)
            }
          >
            CSV Solicitudes
          </Button>
          <Button
            variant="contained"
            startIcon={<DownloadIcon />}
            onClick={() =>
              handleDescargar('/api/admin/reportes/usuarios.csv', `usuarios-${hoy}.csv`)
            }
          >
            CSV Usuarios
          </Button>
        </Box>
      </PageHeader>

      <Grid container spacing={3}>
        {/* Card 1 — Exportar solicitudes */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              borderRadius: 2,
            }}
          >
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <Box
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: 2,
                    bgcolor: '#E8F0FE',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#005EA2',
                  }}
                >
                  <DescriptionIcon />
                </Box>
                <Typography sx={{ fontWeight: 600, fontSize: 16 }}>
                  Solicitudes completas
                </Typography>
              </Box>
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                Exportá todas las solicitudes con estado, fechas y montos.
              </Typography>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<DownloadIcon />}
                onClick={() =>
                  handleDescargar('/api/admin/reportes/solicitudes.csv', `solicitudes-${hoy}.csv`)
                }
              >
                Descargar CSV
              </Button>
            </CardContent>
          </Card>
        </Grid>

        {/* Card 2 — Exportar usuarios */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              borderRadius: 2,
            }}
          >
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <Box
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: 2,
                    bgcolor: '#E6F4EA',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#00A91C',
                  }}
                >
                  <PeopleIcon />
                </Box>
                <Typography sx={{ fontWeight: 600, fontSize: 16 }}>
                  Directorio de usuarios
                </Typography>
              </Box>
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                Listado completo de ciudadanos y empleados registrados.
              </Typography>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<DownloadIcon />}
                onClick={() =>
                  handleDescargar('/api/admin/reportes/usuarios.csv', `usuarios-${hoy}.csv`)
                }
              >
                Descargar CSV
              </Button>
            </CardContent>
          </Card>
        </Grid>

        {/* Card 3 — Resumen estadístico */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              borderRadius: 2,
            }}
          >
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <Box
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: 2,
                    bgcolor: '#FFF8E1',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#FFBE2E',
                  }}
                >
                  <AssessmentIcon />
                </Box>
                <Typography sx={{ fontWeight: 600, fontSize: 16 }}>
                  Resumen del sistema
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                {[
                  { label: 'Total solicitudes', value: dashboard?.totalSolicitudes },
                  { label: 'Ciudadanos', value: dashboard?.totalCiudadanos },
                  { label: 'Empleados', value: dashboard?.totalEmpleados },
                  {
                    label: 'Monto recaudado',
                    value: dashboard
                      ? '$ ' + (dashboard.montoRecaudadoTotal ?? 0).toLocaleString('es-AR')
                      : undefined,
                  },
                ].map(({ label, value }) => (
                  <Box key={label} sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {label}
                    </Typography>
                    {isLoading ? (
                      <Skeleton variant="text" width={60} />
                    ) : (
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {value ?? 0}
                      </Typography>
                    )}
                  </Box>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Card 4 — Por circunscripción */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              borderRadius: 2,
            }}
          >
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <Box
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: 2,
                    bgcolor: '#FDECEA',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#D54309',
                  }}
                >
                  <LocationOnIcon />
                </Box>
                <Typography sx={{ fontWeight: 600, fontSize: 16 }}>
                  Por circunscripción
                </Typography>
              </Box>
              {isLoading ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                  {[1, 2].map((i) => (
                    <Skeleton key={i} variant="rectangular" height={28} sx={{ borderRadius: 1 }} />
                  ))}
                </Box>
              ) : Object.keys(solicitudesPorCirc).length === 0 ? (
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Sin datos disponibles
                </Typography>
              ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                  {Object.entries(solicitudesPorCirc).map(([circ, count]) => {
                    const pct = (count / totalCirc) * 100;
                    return (
                      <Box key={circ}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                          <Typography variant="body2" sx={{ fontSize: 13 }}>
                            Circunscripción {circ}
                          </Typography>
                          <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: 13 }}>
                            {count}
                          </Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={pct}
                          sx={{
                            height: 8,
                            borderRadius: 4,
                            bgcolor: '#F0F0F0',
                            '& .MuiLinearProgress-bar': { bgcolor: '#D54309', borderRadius: 4 },
                          }}
                        />
                      </Box>
                    );
                  })}
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
