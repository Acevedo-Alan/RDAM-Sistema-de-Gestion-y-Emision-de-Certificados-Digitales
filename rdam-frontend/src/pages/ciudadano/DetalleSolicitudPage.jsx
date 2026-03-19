import { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Divider, Snackbar, Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid'
import {
  Timeline, TimelineItem, TimelineSeparator, TimelineConnector,
  TimelineContent, TimelineDot,
} from '@mui/lab';
import CheckCircleOutline from '@mui/icons-material/CheckCircleOutline';
import DownloadIcon from '@mui/icons-material/Download';
import VerifiedIcon from '@mui/icons-material/Verified';
import dayjs from 'dayjs';
import { useAuth } from '../../hooks/useAuth';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmModal from '../../components/common/ConfirmModal';
import estadoColors from '../../utils/estadoColors';
import { formatTarjetaMock } from '../../utils/formatTarjeta';
import { getSolicitud, getHistorialSolicitud, getPagoDatos, cancelarSolicitud } from '../../api/endpoints/solicitudes';
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

export default function DetalleSolicitudPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  const [cancelOpen, setCancelOpen] = useState(false);
  const [pagoDialogOpen, setPagoDialogOpen] = useState(false);
  const [pagoData, setPagoData] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const { user } = useAuth();

  // Invalidar queries cuando volvemos del pago exitoso.
  // Las queries se re-fetchean cuando el usuario navega al detalle sin ?pago=success.
  useEffect(() => {
    const pagoResult = searchParams.get('pago');
    if (pagoResult === 'success') {
      queryClient.invalidateQueries({ queryKey: ['solicitud', id] });
      queryClient.invalidateQueries({ queryKey: ['solicitud-historial', id] });
      queryClient.invalidateQueries({ queryKey: ['mis-solicitudes'] });
    }
    if (pagoResult === 'error') {
      queryClient.invalidateQueries({ queryKey: ['solicitud', id] });
      queryClient.invalidateQueries({ queryKey: ['solicitud-historial', id] });
      queryClient.invalidateQueries({ queryKey: ['mis-solicitudes'] });
      setSnackbar({ open: true, message: 'Hubo un error al procesar el pago', severity: 'error' });
      searchParams.delete('pago');
      setSearchParams(searchParams, { replace: true });
    }
  }, [id]);

  const { data: solicitud, isLoading, isError } = useQuery({
    queryKey: ['solicitud', id],
    queryFn: () => getSolicitud(id).then((r) => r.data),
  });

  const { data: historial } = useQuery({
    queryKey: ['solicitud-historial', id],
    queryFn: () => getHistorialSolicitud(id).then((r) => r.data),
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelarSolicitud(id),
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Solicitud cancelada', severity: 'success' });
      setCancelOpen(false);
      queryClient.invalidateQueries({ queryKey: ['solicitud', id] });
      queryClient.invalidateQueries({ queryKey: ['mis-solicitudes'] });
      setTimeout(() => navigate('/ciudadano/solicitudes'), 1500);
    },
    onError: (error) => {
      const msg = error.response?.data?.message || error.response?.data?.error || 'Error al cancelar la solicitud';
      setSnackbar({ open: true, message: msg, severity: 'error' });
    },
  });

  const handlePago = async () => {
    try {
      const res = await getPagoDatos(id);
      const data = res.data;

      // Crear form dinámico y hacer POST al mock PlusPagos
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = data.urlPasarela;

      const campos = ['Comercio', 'TransaccionComercioId', 'Monto', 'Informacion',
        'CallbackSuccess', 'CallbackCancel', 'UrlSuccess', 'UrlError'];
      campos.forEach((campo) => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = campo;
        input.value = data[campo] ?? '';
        form.appendChild(input);
      });

      document.body.appendChild(form);
      form.submit();
    } catch {
      setSnackbar({ open: true, message: 'Error al obtener datos de pago', severity: 'error' });
    }
  };

  const handleDescargarCertificado = () => {
    window.open(`/api/certificados/${id}/download`, '_blank');
  };

  if (searchParams.get('pago') === 'success') {
    return (
      <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
        {/* Columna izquierda */}
        <Box sx={{
          display: { xs: 'none', md: 'flex' },
          width: '40%',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'flex-start',
          background: 'linear-gradient(160deg, #0F4A7C 0%, #005EA2 50%, #2378C3 100%)',
          px: 8, py: 6,
          gap: 6,
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Box sx={{
              width: 36, height: 36, bgcolor: 'white', borderRadius: '6px',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Typography sx={{ color: '#005EA2', fontWeight: 700, fontSize: 20 }}>R</Typography>
            </Box>
            <Typography sx={{ color: 'white', fontWeight: 700, fontSize: 20 }}>RDAM</Typography>
          </Box>

          <Box>
            <Typography variant="h3" sx={{ color: 'white', fontWeight: 700 }}>
              Sistema de Gestión de Certificados Digitales
            </Typography>
            <Typography variant="body1" sx={{ color: 'rgba(255,255,255,0.75)', mt: 2 }}>
              Siguiente paso: Emisión del documento.
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {[
              'Pago registrado y confirmado',
              'Certificado en proceso de emisión',
              'Comprobante enviado por email',
            ].map((item, idx) => (
              <Box key={idx} sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Box sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: 'rgba(255,255,255,0.5)', flexShrink: 0, mr: 1.5 }} />
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.85)' }}>{item}</Typography>
              </Box>
            ))}
          </Box>

          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)', mt: 'auto' }}>
            Poder Judicial de la Provincia de Santa Fe — 2026
          </Typography>
        </Box>

        {/* Columna derecha */}
        <Box sx={{
          width: { xs: '100%', md: '60%' },
          bgcolor: 'white',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 3, md: 6 },
        }}>
          <Box sx={{ width: '100%', maxWidth: 420, textAlign: 'center' }}>
            <CheckCircleOutline sx={{ fontSize: 64, color: 'success.main', mb: 2 }} />

            <Typography variant="h4" sx={{ fontWeight: 700, color: '#1B1B1B' }}>
              Pago confirmado
            </Typography>

            <Typography variant="body2" sx={{ color: '#71767A', mt: 1, textAlign: 'center' }}>
              Tu solicitud fue actualizada exitosamente. Recibirás un comprobante en tu correo registrado.
            </Typography>

            <Typography variant="caption" sx={{ display: 'block', color: '#A9AEB1', mt: 2, textAlign: 'center' }}>
              Registro de Actos y Documentos del Ámbito de la Magistratura
            </Typography>

            <Button
              variant="contained"
              fullWidth
              onClick={() => {
                queryClient.removeQueries({ queryKey: ['solicitud', id] });
                queryClient.removeQueries({ queryKey: ['solicitud-historial', id] });
                navigate(`/ciudadano/solicitudes/${id}`);
              }}
              sx={{
                mt: 3, py: 1.5, borderRadius: '8px',
                bgcolor: '#005EA2', fontSize: 15, fontWeight: 600,
                textTransform: 'none',
                boxShadow: '0 4px 12px rgba(0,94,162,0.3)',
                '&:hover': { bgcolor: '#0F4A7C' },
              }}
            >
              Ver detalle de la solicitud
            </Button>

            <Typography variant="body2" sx={{ color: '#71767A', mt: 2, textAlign: 'center' }}>
              <Link to="/ciudadano/solicitudes" style={{ color: '#71767A' }}>
                Volver a mis solicitudes
              </Link>
            </Typography>
          </Box>
        </Box>
      </Box>
    );
  }

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

  const puedeIniciarPago = estado === 'PENDIENTE';
  const puedeDescargar = estado === 'PUBLICADO';
  const puedeCancelar = !['PAGADO', 'PUBLICADO', 'PUBLICADO_VENCIDO', 'VENCIDO', 'CANCELADO', 'RECHAZADO'].includes(estado);

  return (
    <>
      <Box sx={{ mb: 2 }}>
        <Button variant="text" onClick={() => navigate('/ciudadano/solicitudes')}>
          &larr; Volver
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
                          Descargar
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
                          {item.createdAt ? dayjs(item.createdAt).format('DD/MM/YYYY HH:mm') : ''}
                        </Typography>
                        {item.comentario && (
                          <Typography variant="body2" sx={{ mt: 0.5 }}>
                            {item.comentario}
                          </Typography>
                        )}
                        {item.estadoNuevo === 'PAGADO' && solicitud?.pago && ['interno', 'admin'].includes(user?.rol) && (
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

      {/* Datos del pago (solo interno/admin en estado PAGADO) */}
      {solicitud.estado === 'PAGADO' && solicitud.pago != null && ['interno', 'admin'].includes(user?.rol) && (
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

      {/* Tarjeta de certificado listo */}
      {puedeDescargar && (
        <Card
          elevation={0}
          sx={{
            mt: 3,
            border: '1px solid #00A91C',
            borderRadius: '8px',
            bgcolor: '#F0FFF4',
          }}
        >
          <CardContent sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
              <Box
                sx={{
                  width: 48, height: 48, borderRadius: '8px',
                  bgcolor: '#E6F4EA', display: 'flex',
                  alignItems: 'center', justifyContent: 'center',
                }}
              >
                <VerifiedIcon sx={{ color: '#00A91C', fontSize: 28 }} />
              </Box>
              <Box>
                <Typography sx={{ fontWeight: 700, fontSize: 16, color: '#1B1B1B' }}>
                  Certificado disponible
                </Typography>
                <Typography variant="body2" sx={{ color: '#71767A' }}>
                  Tu certificado fue emitido y esta listo para descargar.
                </Typography>
              </Box>
            </Box>
            <Button
              variant="contained"
              startIcon={<DownloadIcon />}
              onClick={handleDescargarCertificado}
              sx={{
                bgcolor: '#005EA2',
                borderRadius: '8px',
                textTransform: 'none',
                fontWeight: 600,
                py: 1.2,
                px: 3,
                '&:hover': { bgcolor: '#0F4A7C' },
              }}
            >
              Descargar PDF del certificado
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Botones de accion */}
      <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
        {puedeIniciarPago && (
          <Button variant="contained" color="warning" onClick={handlePago}>
            Iniciar Pago
          </Button>
        )}
        {puedeCancelar && (
          <Button variant="outlined" color="error" onClick={() => setCancelOpen(true)}>
            Cancelar Solicitud
          </Button>
        )}
      </Box>



      {/* Modal confirmar cancelacion */}
      <ConfirmModal
        open={cancelOpen}
        title="Cancelar solicitud"
        message="Esta seguro de que desea cancelar esta solicitud? Esta accion no se puede deshacer."
        onConfirm={() => cancelMutation.mutate()}
        onCancel={() => setCancelOpen(false)}
      />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={5000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          severity={snackbar.severity}
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          variant="filled"
          sx={{ borderRadius: '8px' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
