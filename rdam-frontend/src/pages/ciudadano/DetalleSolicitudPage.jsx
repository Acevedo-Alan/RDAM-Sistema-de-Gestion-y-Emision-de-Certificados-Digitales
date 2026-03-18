import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Snackbar, Typography,
} from '@mui/material';
import {
  Timeline, TimelineItem, TimelineSeparator, TimelineConnector,
  TimelineContent, TimelineDot,
} from '@mui/lab';
import dayjs from 'dayjs';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmModal from '../../components/common/ConfirmModal';
import estadoColors from '../../utils/estadoColors';
import { getSolicitud, getHistorialSolicitud, getPagoDatos, cancelarSolicitud } from '../../api/endpoints/solicitudes';
import { downloadAdjunto } from '../../api/endpoints/adjuntos';

const DOT_COLOR_MAP = {
  PENDIENTE_REVISION: '#FFBE2E',
  EN_REVISION: '#2378C3',
  APROBADA: '#00A91C',
  RECHAZADA: '#D54309',
  PENDIENTE_PAGO: '#936F38',
  PAGADA: '#4D8055',
  EMITIDA: '#0F4A7C',
  CANCELADA: '#71767A',
};

export default function DetalleSolicitudPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [cancelOpen, setCancelOpen] = useState(false);
  const [pagoDialogOpen, setPagoDialogOpen] = useState(false);
  const [pagoData, setPagoData] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

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

  const puedeIniciarPago = estado === 'APROBADA' || estado === 'PENDIENTE_PAGO';
  const puedeDescargar = estado === 'EMITIDA';
  const puedeCancelar = !['PAGADA', 'EMITIDA', 'CANCELADA', 'RECHAZADA'].includes(estado);

  return (
    <>
      <Box sx={{ mb: 2 }}>
        <Button variant="text" onClick={() => navigate('/ciudadano/solicitudes')}>
          &larr; Volver
        </Button>
      </Box>

      <PageHeader title={`Solicitud #${solicitud?.id}`} />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Columna izquierda */}
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" gutterBottom>Datos de la solicitud</Typography>

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              <Typography>
                <strong>N° Solicitud:</strong> {solicitud?.id}
              </Typography>
              <Typography>
                <strong>Tipo de certificado:</strong>{' '}
                {solicitud?.tipoCertificado?.nombre ?? solicitud?.tipoCertificadoNombre ?? ''}
              </Typography>
              <Typography>
                <strong>Circunscripcion:</strong>{' '}
                {solicitud?.circunscripcion?.nombre ?? solicitud?.circunscripcionNombre ?? ''}
              </Typography>
              <Typography>
                <strong>Fecha de creacion:</strong>{' '}
                {solicitud?.fechaCreacion ? dayjs(solicitud.fechaCreacion).format('DD/MM/YYYY') : ''}
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
                      <TimelineDot sx={{ bgcolor: DOT_COLOR_MAP[item.estado] ?? '#71767A' }} />
                      {index < historialItems.length - 1 && <TimelineConnector />}
                    </TimelineSeparator>
                    <TimelineContent>
                      <Box>
                        <StatusBadge estado={item.estado} />
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                          {item.usuario ?? ''}{' '}
                          {item.fecha ? dayjs(item.fecha).format('DD/MM/YYYY HH:mm') : ''}
                        </Typography>
                        {item.observacion && (
                          <Typography variant="body2" sx={{ mt: 0.5 }}>
                            {item.observacion}
                          </Typography>
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

      {/* Botones de accion */}
      <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
        {puedeIniciarPago && (
          <Button variant="contained" color="warning" onClick={handlePago}>
            Iniciar Pago
          </Button>
        )}
        {puedeDescargar && (
          <Button variant="contained" color="success" onClick={handleDescargarCertificado}>
            Descargar Certificado
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
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={snackbar.severity}
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
