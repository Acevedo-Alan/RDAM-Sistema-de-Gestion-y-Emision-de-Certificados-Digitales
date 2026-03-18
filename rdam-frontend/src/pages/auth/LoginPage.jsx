import { useState, useRef } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box, TextField, Button, Typography, Link,
  IconButton, InputAdornment, CircularProgress, Snackbar, Alert,
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import { useMutation } from '@tanstack/react-query';
import { loginApi } from '../../api/endpoints/auth';

export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [waitingOtp, setWaitingOtp] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '' });
  const timerRef = useRef(null);

  const mutation = useMutation({
    mutationFn: (data) => loginApi(data),
    onSuccess: (response) => {
      const email = response.data.email;
      setWaitingOtp(true);
      timerRef.current = setTimeout(() => {
        navigate('/verify', { state: { email } });
      }, 1500);
    },
    onError: (error) => {
      setSnackbar({
        open: true,
        message: error.response?.data?.message || 'Error al iniciar sesion',
      });
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    mutation.mutate({ username, password });
  };

  const handleCancelOtp = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setWaitingOtp(false);
    mutation.reset();
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

          {waitingOtp ? (
            /* Estado Loading */
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <CircularProgress size={48} sx={{ color: '#005EA2', mb: 3 }} />
              <Typography variant="h6" sx={{ fontWeight: 600, color: '#1B1B1B' }}>
                Verificando identidad
              </Typography>
              <Typography variant="body2" sx={{ color: '#71767A', mt: 1 }}>
                Enviando codigo de verificacion a tu email...
              </Typography>
              <Button
                variant="text"
                sx={{ mt: 2, color: '#71767A', textTransform: 'none' }}
                onClick={handleCancelOtp}
              >
                Cancelar
              </Button>
            </Box>
          ) : (
            /* Formulario */
            <Box sx={{ position: 'relative', pb: 12 }}>
              <Typography
                variant="h4"
                sx={{
                  fontWeight: 'bold',
                  color: '#1B1B1B',
                  mb: 1,
                }}
              >
                Bienvenido
              </Typography>
              <Typography variant="body2" sx={{ color: '#71767A', mb: 1 }}>
                Ingresa tus credenciales para acceder al sistema
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
                  id="username"
                  label="Ingrese su CUIL o email"
                  fullWidth
                  required
                  margin="normal"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                    },
                    mt: 0,
                  }}
                />
                <TextField
                  id="password"
                  label="Contrasena"
                  type={showPassword ? 'text' : 'password'}
                  fullWidth
                  required
                  margin="normal"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
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
                          <IconButton
                            onClick={() => setShowPassword(!showPassword)}
                            edge="end"
                            size="small"
                          >
                            {showPassword ? <VisibilityOff /> : <Visibility />}
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
                  Ingresar al sistema
                </Button>
              </form>

              {/* Usuarios de prueba */}
              <Box
                sx={{
                  mt: 4,
                  p: 2,
                  bgcolor: '#F0F0F0',
                  borderRadius: '8px',
                  border: '1px solid #DFE1E2',
                }}
              >
                <Typography variant="caption" sx={{ fontWeight: 600, color: '#71767A', display: 'block', mb: 1 }}>
                  Usuarios de prueba
                </Typography>
                {[
                  { rol: 'Ciudadano', user: '20123456789', pass: 'Test1234' },
                  { rol: 'Interno', user: 'EMP001', pass: '(cualquiera)' },
                  { rol: 'Admin', user: 'ADMIN001', pass: '(cualquiera)' },
                ].map((u) => (
                  <Typography key={u.rol} variant="caption" sx={{ color: '#565C65', display: 'block', lineHeight: 1.8 }}>
                    <strong>{u.rol}:</strong> {u.user} / {u.pass}
                  </Typography>
                ))}
              </Box>

              {/* Footer Link */}
              <Box sx={{ position: 'absolute', bottom: 32, left: 0, right: 0, textAlign: 'center' }}>
                <Typography variant="body2" sx={{ color: '#71767A' }}>
                  Ciudadano?{' '}
                  <Link
                    component={RouterLink}
                    to="/register"
                    sx={{
                      color: '#005EA2',
                      textDecoration: 'none',
                      fontWeight: 600,
                      '&:hover': { textDecoration: 'underline' },
                    }}
                  >
                    Registrate aca
                  </Link>
                </Typography>
              </Box>
            </Box>
          )}
        </Box>
      </Box>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={5000}
        onClose={() => setSnackbar({ open: false, message: '' })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          severity="error"
          onClose={() => setSnackbar({ open: false, message: '' })}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
