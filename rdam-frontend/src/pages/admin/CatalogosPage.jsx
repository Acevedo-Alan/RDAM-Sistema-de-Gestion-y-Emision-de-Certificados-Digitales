import { Box, Card, CardContent, Typography, Grid, Button } from '@mui/material';
import {
  Category as CategoryIcon,
  LocationOn as LocationIcon,
  Description as DocIcon,
  Settings as SettingsIcon,
  Add as AddIcon,
} from '@mui/icons-material';
import PageHeader from '../../components/common/PageHeader';

const catalogos = [
  {
    nombre: 'Tipos de certificado',
    descripcion: 'Gestionar los tipos de certificados disponibles para solicitar.',
    icon: <DocIcon />,
    count: 6,
  },
  {
    nombre: 'Circunscripciones',
    descripcion: 'Administrar las circunscripciones y jurisdicciones del sistema.',
    icon: <LocationIcon />,
    count: 12,
  },
  {
    nombre: 'Categorías',
    descripcion: 'Categorías generales para clasificación de trámites.',
    icon: <CategoryIcon />,
    count: 4,
  },
  {
    nombre: 'Configuración general',
    descripcion: 'Parámetros del sistema, plazos y valores por defecto.',
    icon: <SettingsIcon />,
    count: null,
  },
];

export default function CatalogosPage() {
  return (
    <>
      <PageHeader title="Catálogos" subtitle="Administración de catálogos y configuraciones del sistema">
        <Button variant="contained" startIcon={<AddIcon />}>
          Nuevo catálogo
        </Button>
      </PageHeader>

      <Grid container spacing={3}>
        {catalogos.map((cat) => (
          <Grid size={{ xs: 12, sm: 6, md: 4 }} key={cat.nombre}>
            <Card
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  borderColor: 'primary.main',
                  boxShadow: '0 4px 12px rgba(0,94,162,0.15)',
                },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                  <Box
                    sx={{
                      width: 44,
                      height: 44,
                      borderRadius: 2,
                      bgcolor: '#E8F0FE',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'primary.main',
                    }}
                  >
                    {cat.icon}
                  </Box>
                  <Box sx={{ flex: 1 }}>
                    <Typography sx={{ fontWeight: 600, fontSize: 15, color: 'text.primary' }}>
                      {cat.nombre}
                    </Typography>
                    {cat.count !== null && (
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                        {cat.count} registros
                      </Typography>
                    )}
                  </Box>
                </Box>
                <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: 13 }}>
                  {cat.descripcion}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
