import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Alert, Box, Card, CardContent, CircularProgress, Typography, Divider, Chip } from '@mui/material';
import dayjs from 'dayjs';
import api from '../../api/axios';

export default function VerificarCertificadoPage() {
  const { token } = useParams();

  const { data: resultado, isLoading, isError } = useQuery({
    queryKey: ['verificar-certificado', token],
    queryFn: () => api.get(`/api/verificar/${token}`).then((r) => r.data),
    enabled: !!token,
  });

  return (
    <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      {/* Columna izquierda */}
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          width: '40%',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'flex-start',
          background: 'linear-gradient(160deg, #0F4A7C 0%, #005EA2 50%, #2378C3 100%)',
          px: 8,
          py: 6,
          gap: 6,
        }}
      >
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
            <Typography sx={{ color: '#005EA2', fontWeight: 700, fontSize: 20 }}>R</Typography>
          </Box>
          <Typography sx={{ color: 'white', fontWeight: 700, fontSize: 20 }}>RDAM</Typography>
        </Box>

        <Box>
          <Typography variant="h3" sx={{ color: 'white', fontWeight: 700 }}>
            Sistema de Gestión de Certificados Digitales
          </Typography>
          <Typography variant="body1" sx={{ color: 'rgba(255,255,255,0.75)', mt: 2 }}>
            Verifique aquí la autenticidad de sus certificados.
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {[
            'Verificación segura y auténtica',
            'Certificados digitales válidos',
            'Comprobación inmediata',
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
              <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.85)' }}>
                {item}
              </Typography>
            </Box>
          ))}
        </Box>

        <Typography
          variant="caption"
          sx={{ color: 'rgba(255,255,255,0.5)', mt: 'auto' }}
        >
          Poder Judicial de la Provincia de Santa Fe — 2026
        </Typography>
      </Box>

      {/* Columna derecha */}
      <Box
        sx={{
          width: { xs: '100%', md: '60%' },
          bgcolor: 'white',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 3, md: 6 },
          overflowY: 'auto',
        }}
      >
        <Box sx={{ width: '100%', maxWidth: 420 }}>
          {isLoading && (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
              <CircularProgress />
              <Typography variant="body2" color="text.secondary">
                Verificando certificado...
              </Typography>
            </Box>
          )}

          {isError && (
            <Card sx={{ border: '1px solid #D54309' }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="h5" sx={{ color: '#D54309', fontWeight: 700, mb: 2 }}>
                  ❌ Certificado no válido
                </Typography>
                <Typography variant="body2" sx={{ color: '#71767A' }}>
                  El certificado no fue encontrado o ha expirado.
                </Typography>
              </CardContent>
            </Card>
          )}

          {resultado && resultado.valido && (
            <Card sx={{ border: '1px solid #00A91C', boxShadow: '0 2px 8px rgba(0,169,28,0.15)' }}>
              <CardContent>
                <Box sx={{ textAlign: 'center', mb: 3 }}>
                  <Typography
                    sx={{
                      fontSize: 48,
                      mb: 1,
                    }}
                  >
                    ✓
                  </Typography>
                  <Typography
                    variant="h5"
                    sx={{ color: '#00A91C', fontWeight: 700, mb: 0.5 }}
                  >
                    Certificado Válido
                  </Typography>
                  <Typography variant="body2" sx={{ color: '#71767A' }}>
                    Este certificado es auténtico
                  </Typography>
                </Box>

                <Divider sx={{ my: 2 }} />

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <Box>
                    <Typography variant="caption" sx={{ color: '#71767A', display: 'block', mb: 0.5 }}>
                      Número de Trámite
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, color: '#1B1B1B' }}>
                      {resultado.numeroTramite}
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" sx={{ color: '#71767A', display: 'block', mb: 0.5 }}>
                      Titular
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, color: '#1B1B1B' }}>
                      {resultado.titular}
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" sx={{ color: '#71767A', display: 'block', mb: 0.5 }}>
                      Tipo de Certificado
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, color: '#1B1B1B' }}>
                      {resultado.tipoCertificado}
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" sx={{ color: '#71767A', display: 'block', mb: 0.5 }}>
                      Circunscripción
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, color: '#1B1B1B' }}>
                      {resultado.circunscripcion}
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" sx={{ color: '#71767A', display: 'block', mb: 0.5 }}>
                      Fecha de Emisión
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, color: '#1B1B1B' }}>
                      {resultado.fechaEmision ?? 'No disponible'}
                    </Typography>
                  </Box>
                </Box>

                <Divider sx={{ my: 2 }} />

                <Box sx={{ textAlign: 'center' }}>
                  <Chip label="Certificado Auténtico" color="success" size="small" />
                </Box>
              </CardContent>
            </Card>
          )}

          {resultado && !resultado.valido && (
            <Card sx={{ border: '1px solid #D54309' }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="h5" sx={{ color: '#D54309', fontWeight: 700, mb: 2 }}>
                  ❌ Certificado no válido
                </Typography>
                <Typography variant="body2" sx={{ color: '#71767A' }}>
                  El certificado no fue encontrado o ha expirado.
                </Typography>
              </CardContent>
            </Card>
          )}

          <Typography
            variant="caption"
            sx={{
              display: 'block',
              color: '#A9AEB1',
              mt: 3,
              textAlign: 'center',
            }}
          >
            Registro de Actos y Documentos del Ámbito de la Magistradura
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}
