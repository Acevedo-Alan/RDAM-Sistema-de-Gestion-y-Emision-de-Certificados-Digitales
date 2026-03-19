import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress,
  Divider, Snackbar, Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  Timeline, TimelineItem, TimelineSeparator, TimelineConnector,
  TimelineContent, TimelineDot,
} from '@mui/lab';
import dayjs from 'dayjs';
import { useAuth } from '../../hooks/useAuth';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmModal from '../../components/common/ConfirmModal';
import RechazoModal from '../../components/common/RechazoModal';
import { formatTarjetaMock } from '../../utils/formatTarjeta';
import {
  getSolicitud, getHistorialSolicitud,
  aprobarSolicitud, rechazarSolicitud,
  publicarSolicitud,
} from '../../api/endpoints/solicitudes';
import { downloadAdjunto } from '../../api/endpoints/adjuntos';

const DOT_COLOR_MAP = {
  PENDIENTE: '#FFBE2E',
  PAGADO: '#4D8055',
  PUBLICADO: '#0F4A7C',
  PUBLICADO_VENCIDO: '#565C65',
  VENCIDO: '#D54309',
  RECHAZADO: '#D54309',
  CANCELADO: '#71767A',
};

export default function RevisionSolicitudPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { user } = useAuth();
  const [confirmarAprobarOpen, setConfirmarAprobarOpen] = useState(false);
  const [rechazarModalOpen, setRechazarModalOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const { data: solicitud, isLoading, isError } = useQuery({
    queryKey: ['solicitud-interno', id],
    queryFn: () => getSolicitud(id).then((r) => r.data),
  });

  const { data: historial } = useQuery({
    queryKey: ['solicitud-historial', id],
    queryFn: () => getHistorialSolicitud(id).then((r) => r.data),
  });

  const aprobarMutation = useMutation({
    mutationFn: () => aprobarSolicitud(id),
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Solicitud aprobada correctamente', severity: 'success' });
      setConfirmarAprobarOpen(false);
      queryClient.invalidateQueries({ queryKey: ['solicitud-interno', id] });
      queryClient.invalidateQueries({ queryKey: ['bandeja-solicitudes'] });
      setTimeout(() => navigate('/interno/bandeja'), 1500);
    },
    onError: (error) => {
      const msg = error.response?.data?.message || 'Error al aprobar la solicitud';
      setSnackbar({ open: true, message: msg, severity: 'error' });
    },
  });

  const rechazarMutation = useMutation({
    mutationFn: (motivo) => rechazarSolicitud(id, { motivo }),
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Solicitud rechazada correctamente', severity: 'success' });
      setRechazarModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['solicitud-interno', id] });
      queryClient.invalidateQueries({ queryKey: ['bandeja-solicitudes'] });
      setTimeout(() => navigate('/interno/bandeja'), 1500);
    },
    onError: (error) => {
      const msg = error.response?.data?.message || 'Error al rechazar la solicitud';
      setSnackbar({ open: true, message: msg, severity: 'error' });
    },
  });

  const publicarMutation = useMutation({
    mutationFn: () => publicarSolicitud(id),
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Certificado publicado correctamente', severity: 'success' });
      queryClient.invalidateQueries({ queryKey: ['solicitud-interno', id] });
      queryClient.invalidateQueries({ queryKey: ['solicitud-historial', id] });
      queryClient.invalidateQueries({ queryKey: ['bandeja-solicitudes'] });
    },
    onError: (error) => {
      const msg = error.response?.data?.message || 'Error al publicar el certificado';
      setSnackbar({ open: true, message: msg, severity: 'error' });
    },
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError) {
    return <Alert severity="error">Error al cargar la solicitud</Alert>;
  }

  const estado = solicitud?.estado;
  const adjuntos = solicitud?.adjuntos ?? [];
  const historialItems = Array.isArray(historial) ? historial : [];
  const puedeActuar = estado === 'PENDIENTE';

  return (
    <>
      <Box sx={{ mb: 2 }}>
        <Button variant="text" onClick={() => navigate('/interno/bandeja')}>
          Volver
        </Button>
      </Box>

      <PageHeader title={`Tramite ${solicitud?.numeroTramite ?? solicitud?.id}`} />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Columna izquierda */}
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" gutterBottom>Datos de la solicitud</Typography>

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              <Typography>
                <strong>N° Tramite:</strong> {solicitud?.numeroTramite}
              </Typography>
              <Typography>
                <strong>Tipo de certificado:</strong>{' '}
                {solicitud?.tipoCertificado ?? ''}
              </Typography>
              <Typography>
                <strong>Circunscripcion:</strong>{' '}
                {solicitud?.circunscripcion ?? 'No especificada'}
              </Typography>
              <Typography>
                <strong>Ciudadano:</strong>{' '}
                {`${solicitud?.ciudadano?.apellido ?? ''} ${solicitud?.ciudadano?.nombre ?? ''}`.trim()}
              </Typography>
              <Typography>
                <strong>Fecha de creacion:</strong>{' '}
                {solicitud?.createdAt ? dayjs(solicitud.createdAt).format('DD/MM/YYYY') : ''}
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <strong>Estado:</strong> <StatusBadge estado={estado} />
              </Box>
              {solicitud?.observaciones && (
                <Typography>
                  <strong>Observaciones:</strong> {solicitud.observaciones}
                </Typography>
              )}

              {adjuntos.length > 0 && (
                <Box>
                  <Typography sx={{ fontWeight: 600, mb: 1 }}>Adjuntos:</Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                    {adjuntos.map((adj) => (
                      <Box key={adj.id} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body2">{adj.nombre ?? adj.nombreArchivo ?? `Archivo ${adj.id}`}</Typography>
                        <Button
                          size="small"
                          onClick={() => window.open(downloadAdjunto(adj.id), '_blank')}
                        >
                          Ver
                        </Button>
                      </Box>
                    ))}
                  </Box>
                </Box>
              )}
            </Box>
          </CardContent>
        </Card>

        {/* Columna derecha */}
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" gutterBottom>Historial</Typography>

            {historialItems.length === 0 ? (
              <Typography color="text.secondary">Sin historial disponible</Typography>
            ) : (
              <Timeline position="right" sx={{ p: 0 }}>
                {historialItems.map((item, index) => (
                  <TimelineItem key={index} sx={{ '&::before': { display: 'none' } }}>
                    <TimelineSeparator>
                      <TimelineDot sx={{ bgcolor: DOT_COLOR_MAP[item.estadoNuevo] ?? '#71767A' }} />
                      {index < historialItems.length - 1 && <TimelineConnector />}
                    </TimelineSeparator>
                    <TimelineContent>
                      <Box>
                        <StatusBadge estado={item.estadoNuevo} />
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                          {item.usuarioNombre ?? ''}{' '}
                          {item.createdAt ? dayjs(item.createdAt).format('DD/MM/YYYY HH:mm') : ''}
                        </Typography>
                        {item.comentario && (
                          <Typography variant="body2" sx={{ mt: 0.5 }}>
                            {item.comentario}
                          </Typography>
                        )}
                        {item.estadoNuevo === 'PAGADO' && solicitud?.pago && (
                          <Box sx={{ mt: 0.5, display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                            <Typography variant="caption" color="#71767A">
                              {solicitud.pago.estadoPago === 'APROBADO' ? 'Aprobado' : solicitud.pago.estadoPago} · {solicitud.pago.proveedorPago}
                            </Typography>
                            <Typography variant="caption" color="#71767A">
                              ID: {solicitud.pago.idExterno}
                            </Typography>
                            {solicitud.pago.numeroTarjeta && (
                              <Typography variant="caption" color="#71767A">
                                {formatTarjetaMock(solicitud.pago.numeroTarjeta)}
                              </Typography>
                            )}
                          </Box>
                        )}
                      </Box>
                    </TimelineContent>
                  </TimelineItem>
                ))}
              </Timeline>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Datos del pago */}
      {solicitud.estado === 'PAGADO' && solicitud.pago != null && (
        <Card elevation={0} sx={{ border: '1px solid #DFE1E2', borderRadius: '8px', p: 3, mt: 3 }}>
          <Typography variant="subtitle1" fontWeight={600}>Datos del pago</Typography>
          <Divider sx={{ my: 1.5 }} />
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="#71767A">Estado</Typography>
              <Box><Chip label="Aprobado" color="success" size="small" /></Box>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="#71767A">Proveedor</Typography>
              <Typography variant="body2" fontWeight={500} color="#1B1B1B">{solicitud.pago.proveedorPago}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="#71767A">ID de transacción</Typography>
              <Typography variant="body2" fontWeight={500} color="#1B1B1B">{solicitud.pago.idExterno}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="#71767A">Monto</Typography>
              <Typography variant="body2" fontWeight={500} color="#1B1B1B">
                {new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(solicitud.pago.monto)} ARS
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="#71767A">Fecha de confirmación</Typography>
              <Typography variant="body2" fontWeight={500} color="#1B1B1B">
                {new Date(solicitud.pago.fechaConfirmacion).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
              </Typography>
            </Grid>
          </Grid>
        </Card>
      )}

      {/* Boton publicar certificado */}
      {estado === 'PAGADO' && (
        <Card sx={{ mt: 3, p: 2 }}>
          <Button
            variant="contained"
            onClick={() => publicarMutation.mutate()}
            disabled={publicarMutation.isPending}
            sx={{
              bgcolor: '#005EA2',
              borderRadius: '8px',
              textTransform: 'none',
              fontWeight: 600,
              py: 1.5,
              '&:hover': { bgcolor: '#0F4A7C' },
            }}
          >
            {publicarMutation.isPending ? 'Publicando...' : 'Publicar certificado'}
          </Button>
        </Card>
      )}

      {/* Botones de accion */}
      {puedeActuar && (
        <Card sx={{ mt: 3, p: 2 }}>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
              variant="contained"
              sx={{ bgcolor: '#00A91C', '&:hover': { bgcolor: '#155724' } }}
              onClick={() => setConfirmarAprobarOpen(true)}
            >
              Aprobar
            </Button>
            <Button
              variant="contained"
              sx={{ bgcolor: '#D54309', '&:hover': { bgcolor: '#842029' } }}
              onClick={() => setRechazarModalOpen(true)}
            >
              Rechazar
            </Button>
          </Box>
        </Card>
      )}

      {/* Modal confirmar aprobacion */}
      <ConfirmModal
        open={confirmarAprobarOpen}
        title="Confirmar aprobacion"
        message="Esta accion enviara un email al ciudadano notificando la aprobacion de su solicitud."
        onConfirm={() => aprobarMutation.mutate()}
        onCancel={() => setConfirmarAprobarOpen(false)}
      />

      {/* Modal rechazar */}
      <RechazoModal
        open={rechazarModalOpen}
        onClose={() => setRechazarModalOpen(false)}
        onConfirm={(motivo) => rechazarMutation.mutate(motivo)}
        isLoading={rechazarMutation.isPending}
      />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
