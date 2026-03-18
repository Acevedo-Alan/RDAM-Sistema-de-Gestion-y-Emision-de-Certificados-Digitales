import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box, TextField, Button, Typography, Link,
  IconButton, InputAdornment, Snackbar, Alert,
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import { useMutation } from '@tanstack/react-query';
import { registerApi } from '../../api/endpoints/auth';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    nombre: '',
    cuil: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'error' });

  const setField = (field) => (e) => {
    let value = e.target.value;
    if (field === 'cuil') value = value.replace(/\D/g, '').slice(0, 11);
    setForm({ ...form, [field]: value });
    if (errors[field]) setErrors({ ...errors, [field]: '' });
  };

  const validate = () => {
    const errs = {};
    if (!form.nombre.trim()) errs.nombre = 'Campo obligatorio';
    if (!form.cuil.trim()) errs.cuil = 'Campo obligatorio';
    else if (form.cuil.length !== 11) errs.cuil = 'El CUIL debe tener 11 digitos';
    if (!form.email.trim()) errs.email = 'Campo obligatorio';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = 'Email no valido';
    if (!form.password) errs.password = 'Campo obligatorio';
    if (!form.confirmPassword) errs.confirmPassword = 'Campo obligatorio';
    else if (form.password !== form.confirmPassword) errs.confirmPassword = 'Las contrasenas no coinciden';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const mutation = useMutation({
    mutationFn: (data) => registerApi(data),
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Cuenta creada. Revisa tu email para verificar.', severity: 'success' });
      setTimeout(() => navigate('/login'), 1500);
    },
    onError: (error) => {
      setSnackbar({
        open: true,
        message: error.response?.data?.message || 'Error al registrarse',
        severity: 'error',
      });
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    const { confirmPassword, ...data } = form;
    mutation.mutate(data);
  };

  const LogoComponent = () => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
      <Box
        sx={{
          width: 36,
          height: 36,
          bgcolor: 'white',
          borderRadius: '6px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Typography sx={{ color: '#005EA2', fontWeight: 700, fontSize: 20 }}>
          R
        </Typography>
      </Box>
      <Typography sx={{ color: 'white', fontWeight: 700, fontSize: 18 }}>
        RDAM
      </Typography>
    </Box>
  );

  return (
    <Box
      sx={{
        display: 'flex',
        height: '100vh',
        overflow: 'hidden',
      }}
    >
      {/* Columna Izquierda */}
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          width: '40%',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'flex-start',
          background: 'linear-gradient(160deg, #0F4A7C 0%, #005EA2 50%, #2378C3 100%)',
          p: 6,
          gap: 6,
        }}
      >
        {/* Contenido Superior - Logo */}
        <LogoComponent />

        {/* Contenido Central - Texto Descriptivo */}
        <Box>
          <Typography
            variant="h3"
            sx={{
              color: 'white',
              fontWeight: 700,
              lineHeight: 1.3,
              mb: 2,
            }}
          >
            Sistema de Gestion de Certificados Digitales
          </Typography>
          <Typography
            variant="body1"
            sx={{
              color: 'rgba(255,255,255,0.75)',
              mt: 2,
            }}
          >
            Plataforma oficial para la gestion integral de certificados. Acceso seguro con verificacion en dos pasos.
          </Typography>
        </Box>

        {/* Contenido Inferior - Características */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {[
            'Autenticacion con OTP por email',
            'Gestion de solicitudes en tiempo real',
            'Emision digital de certificados',
          ].map((item, idx) => (
            <Box key={idx} sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <Box
                sx={{
                  width: 6,
                  height: 6,
                  borderRadius: '50%',
                  bgcolor: 'rgba(255,255,255,0.5)',
                  flexShrink: 0,
                  mr: 1.5,
                }}
              />
              <Typography
                variant="body2"
                sx={{
                  color: 'rgba(255,255,255,0.85)',
                }}
              >
                {item}
              </Typography>
            </Box>
          ))}
          <Typography
            variant="caption"
            sx={{
              color: 'rgba(255,255,255,0.5)',
              fontSize: 11,
              mt: 2,
            }}
          >
            Poder Judicial de la Provincia de Santa Fe — 2026
          </Typography>
        </Box>
      </Box>

      {/* Columna Derecha */}
      <Box
        sx={{
          width: { xs: '100%', md: '60%' },
          bgcolor: 'white',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 3, md: 6 },
          overflow: 'auto',
        }}
      >
        <Box sx={{ width: '100%', maxWidth: 420 }}>
          {/* Logo en Mobile */}
          <Box sx={{ display: { xs: 'flex', md: 'none' }, alignItems: 'center', gap: 1, mb: 4 }}>
            <Box
              sx={{
                width: 36,
                height: 36,
                bgcolor: '#005EA2',
                borderRadius: '6px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Typography sx={{ color: 'white', fontWeight: 700, fontSize: 20 }}>
                R
              </Typography>
            </Box>
            <Typography sx={{ color: '#1B1B1B', fontWeight: 700, fontSize: 18 }}>
              RDAM
            </Typography>
          </Box>

          <Typography
            variant="h4"
            sx={{
              fontWeight: 'bold',
              color: '#1B1B1B',
              mb: 1,
            }}
          >
            Crear cuenta
          </Typography>
          <Typography variant="body2" sx={{ color: '#71767A', mb: 1 }}>
            Registro para ciudadanos
          </Typography>
          <Box sx={{ mb: 4, mt: 1 }}>
            <Typography
              variant="caption"
              sx={{
                color: '#A9AEB1',
                display: 'block',
                textAlign: 'center',
              }}
            >
              Registro de Actos y Documentos del Ambito de la Magistratura
            </Typography>
          </Box>

          <form onSubmit={handleSubmit}>
            <TextField
              label="Nombre completo"
              fullWidth
              required
              margin="normal"
              value={form.nombre}
              onChange={setField('nombre')}
              error={!!errors.nombre}
              helperText={errors.nombre}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                },
                mt: 0,
              }}
            />
            <TextField
              label="CUIL"
              fullWidth
              required
              margin="normal"
              value={form.cuil}
              onChange={setField('cuil')}
              error={!!errors.cuil}
              helperText={errors.cuil}
              slotProps={{
                htmlInput: { maxLength: 11, inputMode: 'numeric', pattern: '[0-9]*' },
              }}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                },
                mt: 2,
              }}
            />
            <TextField
              label="Email"
              type="email"
              fullWidth
              required
              margin="normal"
              value={form.email}
              onChange={setField('email')}
              error={!!errors.email}
              helperText={errors.email}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                },
                mt: 2,
              }}
            />
            <TextField
              label="Contrasena"
              type={showPassword ? 'text' : 'password'}
              fullWidth
              required
              margin="normal"
              value={form.password}
              onChange={setField('password')}
              error={!!errors.password}
              helperText={errors.password}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                },
                mt: 2,
              }}
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword(!showPassword)} edge="end" size="small">
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />
            <TextField
              label="Confirmar contrasena"
              type={showConfirm ? 'text' : 'password'}
              fullWidth
              required
              margin="normal"
              value={form.confirmPassword}
              onChange={setField('confirmPassword')}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                },
                mt: 2,
              }}
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowConfirm(!showConfirm)} edge="end" size="small">
                        {showConfirm ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />
            <Button
              type="submit"
              variant="contained"
              fullWidth
              disabled={mutation.isPending}
              sx={{
                mt: 3,
                py: 1.5,
                borderRadius: '8px',
                bgcolor: '#005EA2',
                fontSize: 15,
                fontWeight: 600,
                textTransform: 'none',
                boxShadow: '0 4px 12px rgba(0,94,162,0.3)',
                '&:hover': { bgcolor: '#0F4A7C' },
              }}
            >
              Registrarme
            </Button>
          </form>

          {/* Footer Link */}
          <Box sx={{ position: 'relative', mt: 4, pt: 4, textAlign: 'center', borderTop: '1px solid #E6E6E6' }}>
            <Typography variant="body2" sx={{ color: '#71767A' }}>
              Ya tenes cuenta?{' '}
              <Link
                component={RouterLink}
                to="/login"
                sx={{
                  color: '#005EA2',
                  textDecoration: 'none',
                  fontWeight: 600,
                  '&:hover': { textDecoration: 'underline' },
                }}
              >
                Inicia sesion aca
              </Link>
            </Typography>
          </Box>
        </Box>
      </Box>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={5000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          severity={snackbar.severity}
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
