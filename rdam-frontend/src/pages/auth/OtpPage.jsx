import { useState, useRef, useEffect } from 'react';
import { useLocation, useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box, TextField, Button, Typography, Link, Snackbar, Alert,
} from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { verifyOtpApi } from '../../api/endpoints/auth';
import { useAuth } from '../../hooks/useAuth';

export default function OtpPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { login } = useAuth();
  const email = location.state?.email;

  const [digits, setDigits] = useState(['', '', '', '', '', '']);
  const [snackbar, setSnackbar] = useState({ open: false, message: '' });

  const refs = [
    useRef(null),
    useRef(null),
    useRef(null),
    useRef(null),
    useRef(null),
    useRef(null),
  ];

  useEffect(() => {
    if (!email) navigate('/login', { replace: true });
  }, [email, navigate]);

  const mutation = useMutation({
    mutationFn: (data) => verifyOtpApi(data),
    onSuccess: (response) => {
      login(response.data.token);
    },
    onError: (error) => {
      setSnackbar({
        open: true,
        message: error.response?.data?.message || 'Codigo invalido',
      });
      setDigits(['', '', '', '', '', '']);
      refs[0].current?.focus();
    },
  });

  const handleDigitChange = (index, value) => {
    if (value !== '' && !/^\d$/.test(value)) return;
    const next = [...digits];
    next[index] = value;
    setDigits(next);
    if (value !== '' && index < 5) {
      refs[index + 1].current?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && digits[index] === '' && index > 0) {
      refs[index - 1].current?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;
    const next = [...digits];
    for (let i = 0; i < 6; i++) {
      next[i] = pasted[i] || '';
    }
    setDigits(next);
    const focusIdx = Math.min(pasted.length, 5);
    refs[focusIdx].current?.focus();
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const codigo = digits.join('');
    if (codigo.length !== 6) return;
    mutation.mutate({ email, codigo });
  };

  if (!email) return null;

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
        <LogoComponent />

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

          {/* Ilustracion de sobre */}
          <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3 }}>
            <svg width="80" height="64" viewBox="0 0 80 64" fill="none">
              <rect x="4" y="16" width="72" height="44" rx="4" fill="#E8F1FB" stroke="#005EA2" strokeWidth="2" />
              <path d="M4 20 L40 44 L76 20" stroke="#005EA2" strokeWidth="2" fill="none" />
              <rect x="24" y="4" width="32" height="20" rx="3" fill="#005EA2" opacity="0.15" />
              <rect x="28" y="8" width="24" height="3" rx="1.5" fill="#005EA2" opacity="0.6" />
              <rect x="28" y="14" width="16" height="3" rx="1.5" fill="#005EA2" opacity="0.4" />
            </svg>
          </Box>

          {/* Titulo */}
          <Typography
            variant="h5"
            sx={{ fontWeight: 700, color: '#1B1B1B', textAlign: 'center' }}
          >
            Revisa tu email
          </Typography>

          {/* Subtitulo */}
          <Typography
            variant="body2"
            sx={{ color: '#71767A', textAlign: 'center', mt: 1 }}
          >
            Ingresa el codigo de 6 digitos enviado a
          </Typography>
          <Typography
            variant="body2"
            sx={{ fontWeight: 600, color: '#005EA2', textAlign: 'center', mb: 4 }}
          >
            {email}
          </Typography>

          {/* Formulario OTP */}
          <form onSubmit={handleSubmit}>
            <Box sx={{ display: 'flex', gap: 1.5, justifyContent: 'center', mb: 3 }}>
              {digits.map((digit, i) => (
                <TextField
                  key={i}
                  value={digit}
                  onChange={(e) => handleDigitChange(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  onPaste={i === 0 ? handlePaste : undefined}
                  inputRef={refs[i]}
                  slotProps={{
                    htmlInput: {
                      maxLength: 1,
                      inputMode: 'numeric',
                      style: {
                        textAlign: 'center',
                        fontSize: 24,
                        fontWeight: 700,
                        padding: '12px 0',
                      },
                    },
                  }}
                  sx={{
                    width: 48,
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                    },
                  }}
                />
              ))}
            </Box>

            <Button
              type="submit"
              variant="contained"
              fullWidth
              disabled={mutation.isPending || digits.filter((d) => d !== '').length !== 6}
              sx={{
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
              Verificar codigo
            </Button>
          </form>

          {/* Volver al inicio */}
          <Typography variant="body2" sx={{ textAlign: 'center', mt: 3 }}>
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
              Volver al inicio
            </Link>
          </Typography>
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
