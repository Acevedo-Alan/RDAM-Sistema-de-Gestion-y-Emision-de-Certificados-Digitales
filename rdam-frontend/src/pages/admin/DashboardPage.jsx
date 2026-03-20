import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Skeleton,
  Chip,
  LinearProgress,
  Divider,
} from '@mui/material';
import {
  People as PeopleIcon,
  Description as DescriptionIcon,
  AttachMoney as AttachMoneyIcon,
  Today as TodayIcon,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import api from '../../api/axios';
import PageHeader from '../../components/common/PageHeader';

const estadoColors = {
  PENDIENTE: '#FFBE2E',
  PAGADO: '#005EA2',
  PUBLICADO: '#00A91C',
  RECHAZADO: '#D54309',
  CANCELADO: '#71767A',
};

const estadoChipSx = {
  PENDIENTE: { bgcolor: '#FFF8E1', color: '#FFBE2E' },
  PAGADO: { bgcolor: '#E8F0FE', color: '#005EA2' },
  PUBLICADO: { bgcolor: '#E6F4EA', color: '#00A91C' },
  RECHAZADO: { bgcolor: '#FDECEA', color: '#D54309' },
  CANCELADO: { bgcolor: '#F0F0F0', color: '#71767A' },
};

export default function DashboardPage() {
  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard-admin'],
    queryFn: () =>
      api.get('/api/dashboard/admin').then((r) => r.data).catch(() => null),
    refetchInterval: 60000,
  });

  const { data: recentData } = useQuery({
    queryKey: ['admin-solicitudes-recientes'],
    queryFn: () =>
      api
        .get('/api/admin/solicitudes?size=5&sort=createdAt,desc')
        .then((r) => r.data)
        .catch(() => null),
    refetchInterval: 60000,
  });

  const solicitudesPorEstado = dashboard?.solicitudesPorEstado ?? {};
  const totalSolicitudes = dashboard?.totalSolicitudes ?? 0;
  const solicitudes = recentData?.content ?? [];

  const kpis = [
    {
      label: 'Total solicitudes',
      value: dashboard?.totalSolicitudes ?? 0,
      icon: <DescriptionIcon />,
      color: '#005EA2',
      bg: '#E8F0FE',
    },
    {
      label: 'Ciudadanos registrados',
      value: dashboard?.totalCiudadanos ?? 0,
      icon: <PeopleIcon />,
      color: '#00A91C',
      bg: '#E6F4EA',
    },
    {
      label: 'Monto recaudado',
      value: dashboard
        ? '$ ' + (dashboard.montoRecaudadoTotal ?? 0).toLocaleString('es-AR')
        : '$ 0',
      icon: <AttachMoneyIcon />,
      color: '#FFBE2E',
      bg: '#FFF8E1',
    },
    {
      label: 'Solicitudes hoy',
      value: dashboard?.solicitudesHoy ?? 0,
      icon: <TodayIcon />,
      color: '#D54309',
      bg: '#FDECEA',
    },
  ];

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Resumen general del sistema RDAM" />

      <Grid container spacing={3}>
        {kpis.map((kpi) => (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }} key={kpi.label}>
            <Card
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
                borderRadius: 2,
              }}
            >
              <CardContent sx={{ p: 3, '&:last-child': { pb: 3 } }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: 13, mb: 1 }}>
                      {kpi.label}
                    </Typography>
                    {isLoading ? (
                      <Skeleton variant="text" width={80} height={40} />
                    ) : (
                      <Typography variant="h4" sx={{ fontWeight: 700, color: 'text.primary', fontSize: 28 }}>
                        {kpi.value}
                      </Typography>
                    )}
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
        {/* Gráfico de solicitudes por estado */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              borderRadius: 2,
              height: 320,
            }}
          >
            <CardContent sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography sx={{ fontWeight: 600, fontSize: 16, mb: 2.5 }}>
                Solicitudes por estado
              </Typography>
              {isLoading ? (
                <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2, justifyContent: 'center' }}>
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} variant="rectangular" height={32} sx={{ borderRadius: 1 }} />
                  ))}
                </Box>
              ) : Object.keys(solicitudesPorEstado).length === 0 ? (
                <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography color="text.secondary" fontSize={14}>
                    Sin datos disponibles
                  </Typography>
                </Box>
              ) : (
                <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2, justifyContent: 'center' }}>
                  {Object.entries(solicitudesPorEstado).map(([estado, count]) => {
                    const pct = totalSolicitudes > 0 ? (count / totalSolicitudes) * 100 : 0;
                    const color = estadoColors[estado] ?? '#71767A';
                    return (
                      <Box key={estado}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                          <Typography variant="body2" sx={{ fontWeight: 500, fontSize: 13 }}>
                            {estado}
                          </Typography>
                          <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: 13 }}>
                            {count}
                          </Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={pct}
                          sx={{
                            height: 10,
                            borderRadius: 5,
                            bgcolor: '#F0F0F0',
                            '& .MuiLinearProgress-bar': { bgcolor: color, borderRadius: 5 },
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

        {/* Actividad reciente */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
              borderRadius: 2,
              height: 320,
            }}
          >
            <CardContent sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography sx={{ fontWeight: 600, fontSize: 16, mb: 2 }}>
                Actividad reciente
              </Typography>
              {solicitudes.length === 0 ? (
                <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography color="text.secondary" fontSize={14}>
                    Sin solicitudes recientes
                  </Typography>
                </Box>
              ) : (
                <Box sx={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 0 }}>
                  {solicitudes.map((s, idx) => (
                    <Box key={s.id ?? idx}>
                      <Box sx={{ py: 1.2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Box>
                          <Typography variant="body2" sx={{ fontWeight: 600, fontSize: 12 }}>
                            {s.numeroTramite}
                          </Typography>
                          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                            {s.createdAt ? dayjs(s.createdAt).format('DD/MM/YYYY HH:mm') : '—'}
                          </Typography>
                        </Box>
                        <Chip
                          label={s.estado}
                          size="small"
                          sx={{
                            fontSize: 11,
                            fontWeight: 600,
                            height: 22,
                            ...(estadoChipSx[s.estado] ?? { bgcolor: '#F0F0F0', color: '#71767A' }),
                          }}
                        />
                      </Box>
                      {idx < solicitudes.length - 1 && (
                        <Divider sx={{ borderColor: '#F0F0F0' }} />
                      )}
                    </Box>
                  ))}
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
