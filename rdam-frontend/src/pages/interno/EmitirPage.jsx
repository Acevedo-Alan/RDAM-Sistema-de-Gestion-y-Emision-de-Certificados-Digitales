import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, CircularProgress,
  Snackbar, Typography,
} from '@mui/material';
import dayjs from 'dayjs';
import PageHeader from '../../components/common/PageHeader';
import { getSolicitud, emitirCertificado } from '../../api/endpoints/solicitudes';

export default function EmitirPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [certificadoId, setCertificadoId] = useState(null);

  const { data: solicitud, isLoading, isError } = useQuery({
    queryKey: ['solicitud-emitir', id],
    queryFn: () => getSolicitud(id).then((r) => r.data),
  });

  const emitirMutation = useMutation({
    mutationFn: () => emitirCertificado(id),
    onSuccess: (res) => {
      setCertificadoId(res.data?.id || id);
      setSnackbar({ open: true, message: 'Certificado emitido correctamente', severity: 'success' });
    },
    onError: (error) => {
      const msg = error.response?.data?.message || 'Error al emitir el certificado';
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

  if (solicitud?.estado !== 'PAGADA') {
    return (
      <>
        <Box sx={{ mb: 2 }}>
          <Button variant="text" onClick={() => navigate(`/interno/solicitudes/${id}`)}>
            Volver
          </Button>
        </Box>
        <Alert severity="error">
          Esta solicitud no se puede emitir. Debe estar en estado PAGADA.
        </Alert>
      </>
    );
  }

  return (
    <>
      <Box sx={{ mb: 2 }}>
        <Button variant="text" onClick={() => navigate(`/interno/solicitudes/${id}`)}>
          Volver
        </Button>
      </Box>

      <PageHeader title="Emitir certificado" />

      <Card sx={{ maxWidth: 600, mx: 'auto', border: '1px solid #DCDEE0', borderRadius: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Resumen de la solicitud
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 3 }}>
            <Box>
              <Typography variant="body2" color="text.secondary">Tipo de certificado</Typography>
              <Typography variant="body1">
                {solicitud?.tipoCertificado?.nombre ?? solicitud?.tipoCertificadoNombre ?? ''}
              </Typography>
            </Box>

            <Box>
              <Typography variant="body2" color="text.secondary">Ciudadano</Typography>
              <Typography variant="body1">
                {`${solicitud?.ciudadano?.apellido ?? ''} ${solicitud?.ciudadano?.nombre ?? ''}`.trim()}
              </Typography>
            </Box>

            <Box>
              <Typography variant="body2" color="text.secondary">Circunscripcion</Typography>
              <Typography variant="body1">
                {solicitud?.circunscripcion?.nombre ?? solicitud?.circunscripcionNombre ?? ''}
              </Typography>
            </Box>

            <Box>
              <Typography variant="body2" color="text.secondary">Fecha de pago</Typography>
              <Typography variant="body1">
                {solicitud?.fechaPago ? dayjs(solicitud.fechaPago).format('DD/MM/YYYY') : 'No disponible'}
              </Typography>
            </Box>
          </Box>

          <Alert severity="info" sx={{ mb: 2 }}>
            Al emitir el certificado se generara el PDF y se enviara por email al ciudadano.
          </Alert>

          {!certificadoId ? (
            <Button
              fullWidth
              variant="contained"
              sx={{ bgcolor: '#005EA2', '&:hover': { bgcolor: '#0F4A7C' }, py: 1.5 }}
              onClick={() => emitirMutation.mutate()}
              disabled={emitirMutation.isPending}
            >
              {emitirMutation.isPending ? <CircularProgress size={20} sx={{ mr: 1 }} /> : null}
              Emitir certificado
            </Button>
          ) : (
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button
                fullWidth
                variant="contained"
                sx={{ bgcolor: '#005EA2', '&:hover': { bgcolor: '#0F4A7C' }, py: 1.5 }}
                onClick={() => window.open(`/api/certificados/${certificadoId}/download`, '_blank')}
              >
                Descargar PDF
              </Button>
              <Button
                fullWidth
                variant="outlined"
                onClick={() => navigate(`/interno/solicitudes/${id}`)}
              >
                Volver
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>

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
