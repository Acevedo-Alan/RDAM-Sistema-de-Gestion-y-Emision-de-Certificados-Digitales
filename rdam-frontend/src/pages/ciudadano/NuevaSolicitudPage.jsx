import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Box, Button, Card, Chip, CircularProgress,
  FormControl, InputLabel, MenuItem, Select, Snackbar, Alert,
  Step, StepLabel, Stepper, TextField, Typography,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import PageHeader from '../../components/common/PageHeader';
import { CIRCUNSCRIPCIONES_OPTIONS } from '../../utils/formatters';
import { getTiposCertificado } from '../../api/endpoints/catalogos';
import { crearSolicitud } from '../../api/endpoints/solicitudes';
import { uploadAdjunto } from '../../api/endpoints/adjuntos';

const pasos = ['Datos', 'Adjuntos', 'Confirmar'];
const MAX_FILES = 3;
const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ACCEPTED_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];

export default function NuevaSolicitudPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [activeStep, setActiveStep] = useState(0);
  const [tipoCertificadoId, setTipoCertificadoId] = useState('');
  const [circunscripcionId, setCircunscripcionId] = useState('');
  const [observaciones, setObservaciones] = useState('');
  const [archivos, setArchivos] = useState([]);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'error' });

  const { data: tiposData } = useQuery({
    queryKey: ['tipos-certificado'],
    queryFn: () => getTiposCertificado().then((r) => r.data),
  });

  const tipos = Array.isArray(tiposData) ? tiposData : [];

  const tipoSeleccionado = tipos.find((t) => t.id === tipoCertificadoId);
  const circunscripcionSeleccionada = CIRCUNSCRIPCIONES_OPTIONS.find((c) => c.value === circunscripcionId);

  const enviarMutation = useMutation({
    mutationFn: async () => {
      const res = await crearSolicitud({
        tipoCertificadoId: Number(tipoCertificadoId),
        circunscripcionId,
        observaciones,
      });
      const solicitudId = res.data.id;

      for (const archivo of archivos) {
        await uploadAdjunto(solicitudId, archivo);
      }

      return res.data;
    },
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Solicitud creada exitosamente', severity: 'success' });
      setTimeout(() => navigate('/ciudadano/solicitudes'), 1500);
    },
    onError: (error) => {
      const msg = error.response?.data?.message || error.response?.data?.error || 'Error al crear la solicitud';
      setSnackbar({ open: true, message: msg, severity: 'error' });
    },
  });

  const handleNext = () => {
    if (activeStep === 0) {
      if (!tipoCertificadoId || !circunscripcionId) {
        setSnackbar({ open: true, message: 'Tipo de certificado y circunscripcion son obligatorios', severity: 'error' });
        return;
      }
    }
    setActiveStep((prev) => prev + 1);
  };

  const handleBack = () => setActiveStep((prev) => prev - 1);

  const handleFileSelect = (e) => {
    const nuevos = Array.from(e.target.files);
    e.target.value = '';

    if (archivos.length + nuevos.length > MAX_FILES) {
      setSnackbar({ open: true, message: `Maximo ${MAX_FILES} archivos permitidos`, severity: 'error' });
      return;
    }

    for (const file of nuevos) {
      if (file.size > MAX_FILE_SIZE) {
        setSnackbar({ open: true, message: `El archivo "${file.name}" supera los 5MB`, severity: 'error' });
        return;
      }
      if (!ACCEPTED_TYPES.includes(file.type)) {
        setSnackbar({ open: true, message: `Formato no permitido: "${file.name}". Solo PDF, JPG, PNG`, severity: 'error' });
        return;
      }
    }

    setArchivos((prev) => [...prev, ...nuevos]);
  };

  const removeFile = (index) => {
    setArchivos((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <>
      <PageHeader title="Nueva solicitud" />

      <Stepper activeStep={activeStep} sx={{ mb: 4, maxWidth: 600, mx: 'auto', '& .MuiStepLabel-label': { fontSize: 13 } }}>
        {pasos.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {activeStep === 0 && (
        <Card sx={{ p: 4, boxShadow: '0 1px 3px rgba(0,0,0,0.08)', border: '1px solid #DCDEE0', maxWidth: 600, mx: 'auto', width: '100%' }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <FormControl fullWidth required>
              <InputLabel>Tipo de Certificado</InputLabel>
              <Select
                value={tipoCertificadoId}
                label="Tipo de Certificado"
                onChange={(e) => setTipoCertificadoId(e.target.value)}
              >
                {tipos.map((t) => (
                  <MenuItem key={t.id} value={t.id}>{t.nombre}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth required>
              <InputLabel>Circunscripcion</InputLabel>
              <Select
                value={circunscripcionId}
                label="Circunscripcion"
                onChange={(e) => setCircunscripcionId(e.target.value)}
              >
                <MenuItem value="" disabled>
                  Selecciona una circunscripcion
                </MenuItem>
                {CIRCUNSCRIPCIONES_OPTIONS.map((op) => (
                  <MenuItem key={op.value} value={op.value}>
                    {op.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Observaciones"
              multiline
              rows={4}
              value={observaciones}
              onChange={(e) => setObservaciones(e.target.value)}
              inputProps={{ maxLength: 500 }}
              helperText={`${observaciones.length}/500`}
            />

            <Button variant="contained" fullWidth onClick={handleNext} sx={{ bgcolor: '#005EA2', '&:hover': { bgcolor: '#0F4A7C' } }}>
              Siguiente
            </Button>
          </Box>
        </Card>
      )}

      {activeStep === 1 && (
        <Card sx={{ p: 4, boxShadow: '0 1px 3px rgba(0,0,0,0.08)', border: '1px solid #DCDEE0', maxWidth: 600, mx: 'auto', width: '100%' }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Box
              onClick={() => fileInputRef.current?.click()}
              sx={{
                border: '2px dashed #A9AEB1',
                borderRadius: '8px',
                padding: '32px',
                textAlign: 'center',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 1.5,
                '&:hover': { borderColor: '#005EA2', bgcolor: '#F9F9F9' },
                transition: 'all 0.2s',
              }}
            >
              <CloudUploadIcon sx={{ fontSize: 40, color: '#A9AEB1' }} />
              <Typography variant="body1" fontWeight={500} color="#1B1B1B">
                Arrastra archivos aqui o hace click para seleccionar
              </Typography>
              <Typography variant="caption" color="#71767A">
                PDF, JPG, PNG - Max 5MB por archivo - Max 3 archivos
              </Typography>
              <input
                ref={fileInputRef}
                type="file"
                hidden
                accept=".pdf,.jpg,.jpeg,.png"
                multiple
                onChange={handleFileSelect}
              />
            </Box>

            {archivos.length > 0 && (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {archivos.map((file, i) => (
                  <Chip
                    key={i}
                    label={file.name}
                    onDelete={() => removeFile(i)}
                    deleteIcon={<CloseIcon />}
                  />
                ))}
              </Box>
            )}

            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button variant="outlined" fullWidth onClick={handleBack}>
                Atras
              </Button>
              <Button variant="contained" fullWidth onClick={handleNext} sx={{ bgcolor: '#005EA2', '&:hover': { bgcolor: '#0F4A7C' } }}>
                Siguiente
              </Button>
            </Box>
          </Box>
        </Card>
      )}

      {activeStep === 2 && (
        <Card sx={{ p: 4, boxShadow: '0 1px 3px rgba(0,0,0,0.08)', border: '1px solid #DCDEE0', maxWidth: 600, mx: 'auto', width: '100%' }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Card variant="outlined" sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>Resumen de la solicitud</Typography>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <Typography>
                  <strong>Tipo de certificado:</strong> {tipoSeleccionado?.nombre ?? ''}
                </Typography>
                <Typography>
                  <strong>Circunscripcion:</strong> {circunscripcionSeleccionada?.label ?? ''}
                </Typography>
                {observaciones && (
                  <Typography>
                    <strong>Observaciones:</strong> {observaciones}
                  </Typography>
                )}
                <Typography>
                  <strong>Adjuntos:</strong> {archivos.length} archivo{archivos.length !== 1 ? 's' : ''}
                </Typography>
              </Box>
            </Card>

            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button variant="outlined" fullWidth onClick={handleBack}>
                Atras
              </Button>
              <Button
                variant="contained"
                fullWidth
                onClick={() => enviarMutation.mutate()}
                disabled={enviarMutation.isPending}
                sx={{ bgcolor: '#005EA2', '&:hover': { bgcolor: '#0F4A7C' } }}
              >
                {enviarMutation.isPending ? <CircularProgress size={24} /> : 'Enviar solicitud'}
              </Button>
            </Box>
          </Box>
        </Card>
      )}

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
