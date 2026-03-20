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

  const handleDescargarCertificado = async () => {
  const certId = solicitud?.certificadoId;
  if (!certId) return;
  try {
    const token = localStorage.getItem('accessToken');
    const response = await fetch(`/api/certificados/${certId}/download`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) throw new Error('Error al descargar');
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `certificado-${solicitud.numeroTramite}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch {
    setSnackbar({ open: true, message: 'Error al descargar el certificado', severity: 'error' });
  }
};

if (searchParams.get('pago') === 'success') {
  return (
    <Box sx={{
      minHeight: '100vh',
      bgcolor: 'background.default',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      p: 3,
    }}>
      <Box sx={{
        bgcolor: 'background.paper',
        borderRadius: 3,
        boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
        p: { xs: 3, md: 5 },
        width: '100%',
        maxWidth: 480,
        textAlign: 'center',
      }}>
        {/* Ícono de éxito */}
        <Box sx={{
          width: 72,
          height: 72,
          borderRadius: '50%',
          bgcolor: '#ECFDF3',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mx: 'auto',
          mb: 3,
        }}>
          <CheckCircleOutline sx={{ fontSize: 42, color: '#00A91C' }} />
        </Box>

        {/* Título */}
        <Typography variant="h5" sx={{ fontWeight: 700, color: 'text.primary', mb: 1 }}>
          Pago confirmado
        </Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
          Tu solicitud fue actualizada exitosamente. Recibirás un comprobante en tu correo registrado.
        </Typography>

        {/* Resumen */}
        <Box sx={{
          bgcolor: '#EEF4FF',
          borderLeft: '3px solid #005EA2',
          borderRadius: 1,
          p: 2,
          mb: 3,
          textAlign: 'left',
        }}>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 1, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>
            Resumen
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="body2" sx={{ color: 'text.primary' }}>Estado</Typography>
            <Typography variant="body2" sx={{ color: '#00A91C', fontWeight: 700 }}>Pagado</Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
            <Typography variant="body2" sx={{ color: 'text.primary' }}>N° Solicitud</Typography>
            <Typography variant="body2" sx={{ color: 'text.primary', fontWeight: 600 }}>{id}</Typography>
          </Box>
        </Box>

        {/* Checklist */}
        <Box sx={{ mb: 3, textAlign: 'left' }}>
          {[
            'Pago registrado y confirmado',
            'Certificado en proceso de emisión',
            'Comprobante enviado por email',
          ].map((item, idx) => (
            <Box key={idx} sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
              <CheckCircleOutline sx={{ fontSize: 18, color: '#00A91C' }} />
              <Typography variant="body2" sx={{ color: 'text.primary' }}>{item}</Typography>
            </Box>
          ))}
        </Box>

        {/* Botones */}
        <Button
          variant="contained"
          fullWidth
          onClick={() => navigate(`/ciudadano/solicitudes/${id}`, { replace: true })}
          sx={{
            bgcolor: '#005EA2',
            borderRadius: '8px',
            py: 1.5,
            mb: 1.5,
            '&:hover': { bgcolor: '#0F4A7C' },
          }}
        >
          Ver detalle de la solicitud
        </Button>
        <Button
          variant="text"
          fullWidth
          onClick={() => navigate('/ciudadano/solicitudes')}
          sx={{ color: 'text.secondary' }}
        >
          Volver a mis solicitudes
        </Button>
      </Box>

      {/* Footer */}
      <Typography variant="caption" sx={{ color: 'text.disabled', mt: 3 }}>
        Registro de Actos y Documentos del Ámbito de la Magistradura
      </Typography>
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
                            <Typography variant="caption" color="text.secondary">
                              {solicitud.pago.estadoPago === 'APROBADO' ? 'Aprobado' : solicitud.pago.estadoPago} · {solicitud.pago.proveedorPago}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID: {solicitud.pago.idExterno}
                            </Typography>
                            {solicitud.pago.numeroTarjeta && (
                              <Typography variant="caption" color="text.secondary">
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
        <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: '8px', p: 3, mt: 3 }}>
          <Typography variant="subtitle1" fontWeight={600}>Datos del pago</Typography>
          <Divider sx={{ my: 1.5 }} />
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="text.secondary">Estado</Typography>
              <Box><Chip label="Aprobado" color="success" size="small" /></Box>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="text.secondary">Proveedor</Typography>
              <Typography variant="body2" fontWeight={500} color="text.primary">{solicitud.pago.proveedorPago}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="text.secondary">ID de transacción</Typography>
              <Typography variant="body2" fontWeight={500} color="text.primary">{solicitud.pago.idExterno}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="text.secondary">Monto</Typography>
              <Typography variant="body2" fontWeight={500} color="text.primary">
                {new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(solicitud.pago.monto)} ARS
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="text.secondary">Fecha de confirmación</Typography>
              <Typography variant="body2" fontWeight={500} color="text.primary">
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
                <Typography sx={{ fontWeight: 700, fontSize: 16, color: 'text.primary' }}>
                  Certificado disponible
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
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
