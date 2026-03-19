import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, TextField, Select, MenuItem,
  FormControl, InputLabel, CircularProgress, Snackbar,
  Typography,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { LocalizationProvider, DatePicker } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import {
  getBandeja, tomarSolicitud, publicarSolicitud,
} from '../../api/endpoints/solicitudes';

export default function BandejaPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [filtros, setFiltros] = useState({
    search: '',
    estado: 'Todos',
    desde: null,
    hasta: null,
  });
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['bandeja-solicitudes', filtros],
    queryFn: () => getBandeja().then((r) => r.data),
  });

  const tomarMutation = useMutation({
    mutationFn: (id) => tomarSolicitud(id),
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Solicitud tomada correctamente', severity: 'success' });
      queryClient.invalidateQueries({ queryKey: ['bandeja-solicitudes'] });
    },
    onError: (error) => {
      const msg = error.response?.data?.message || 'Error al tomar la solicitud';
      setSnackbar({ open: true, message: msg, severity: 'error' });
    },
  });

  const publicarMutation = useMutation({
    mutationFn: (id) => publicarSolicitud(id),
    onSuccess: () => {
      setSnackbar({ open: true, message: 'Certificado emitido correctamente', severity: 'success' });
      queryClient.invalidateQueries({ queryKey: ['bandeja-solicitudes'] });
    },
    onError: (error) => {
      const msg = error.response?.data?.message || 'Error al emitir el certificado';
      setSnackbar({ open: true, message: msg, severity: 'error' });
    },
  });

  const solicitudes = Array.isArray(data) ? data : data?.content ?? [];

  const solicitudesConDias = solicitudes.map((sol) => ({
    ...sol,
    diasEnEstado: dayjs().diff(dayjs(sol.updatedAt), 'day'),
  }));

  const handleLimpiarFiltros = () => {
    setFiltros({ search: '', estado: 'Todos', desde: null, hasta: null });
  };

  const columns = [
    {
      field: 'numeroTramite',
      headerName: 'N° Tramite',
      width: 200,
      valueGetter: (value, row) => row.numeroTramite ?? row.id,
    },
    {
      field: 'ciudadano',
      headerName: 'Ciudadano',
      flex: 1,
      valueGetter: (value, row) => {
        const nombre = row.ciudadano?.nombre || row.ciudadanoNombre || '';
        const apellido = row.ciudadano?.apellido || row.ciudadanoApellido || '';
        return `${apellido} ${nombre}`.trim();
      },
    },
    {
      field: 'tipoCertificado',
      headerName: 'Tipo Certificado',
      flex: 1,
      valueGetter: (value, row) => row.tipoCertificado?.nombre ?? row.tipoCertificadoNombre ?? value ?? '',
    },
    {
      field: 'circunscripcion',
      headerName: 'Circunscripcion',
      width: 150,
      valueGetter: (value, row) => row.circunscripcion ?? value ?? 'No especificada',
    },
    {
      field: 'fechaCreacion',
      headerName: 'Fecha',
      width: 130,
      valueFormatter: (value) => value ? dayjs(value).format('DD/MM/YYYY') : '',
    },
    {
      field: 'diasEnEstado',
      headerName: 'Dias en estado',
      width: 120,
      sortable: false,
      filterable: false,
    },
    {
      field: 'estado',
      headerName: 'Estado',
      width: 160,
      renderCell: (params) => <StatusBadge estado={params.value} />,
    },
    {
      field: 'acciones',
      headerName: 'Acciones',
      width: 240,
      sortable: false,
      filterable: false,
      renderCell: (params) => {
        const estado = params.row.estado;
        return (
          <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center', height: '100%' }}>
            <Button
              size="small"
              variant="outlined"
              onClick={() => navigate(`/interno/solicitudes/${params.row.id}`)}
            >
              Ver
            </Button>
            {(estado === 'PENDIENTE' || estado === 'PENDIENTE_REVISION') && (
              <Button
                size="small"
                variant="contained"
                onClick={() => tomarMutation.mutate(params.row.id)}
                disabled={tomarMutation.isPending}
              >
                Tomar
              </Button>
            )}
            {estado === 'PAGADO' && (
              <Button
                size="small"
                variant="contained"
                color="success"
                onClick={() => publicarMutation.mutate(params.row.id)}
                disabled={publicarMutation.isPending}
              >
                Emitir
              </Button>
            )}
          </Box>
        );
      },
    },
  ];

  if (isError) {
    return <Alert severity="error">Error al cargar las solicitudes</Alert>;
  }

  return (
    <>
      <PageHeader title="Bandeja de solicitudes" />

      <Card sx={{ border: '1px solid #DCDEE0', borderRadius: 2, p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <TextField
            size="small"
            label="Buscar por N° o ciudadano"
            value={filtros.search}
            onChange={(e) => setFiltros({ ...filtros, search: e.target.value })}
            sx={{ width: 280 }}
          />

          <FormControl sx={{ width: 150 }} size="small">
            <InputLabel>Estado</InputLabel>
            <Select
              value={filtros.estado}
              onChange={(e) => setFiltros({ ...filtros, estado: e.target.value })}
              label="Estado"
            >
              <MenuItem value="Todos">Todos</MenuItem>
              <MenuItem value="PENDIENTE">Pendiente</MenuItem>
              <MenuItem value="PAGADO">Pagado</MenuItem>
              <MenuItem value="PUBLICADO">Publicado</MenuItem>
              <MenuItem value="PUBLICADO_VENCIDO">Publicado vencido</MenuItem>
              <MenuItem value="VENCIDO">Vencido</MenuItem>
              <MenuItem value="RECHAZADO">Rechazado</MenuItem>
              <MenuItem value="CANCELADO">Cancelado</MenuItem>
            </Select>
          </FormControl>

          <LocalizationProvider dateAdapter={AdapterDayjs}>
            <DatePicker
              label="Desde"
              value={filtros.desde}
              onChange={(date) => setFiltros({ ...filtros, desde: date })}
              slotProps={{ textField: { size: 'small', sx: { width: 150 } } }}
            />
            <DatePicker
              label="Hasta"
              value={filtros.hasta}
              onChange={(date) => setFiltros({ ...filtros, hasta: date })}
              slotProps={{ textField: { size: 'small', sx: { width: 150 } } }}
            />
          </LocalizationProvider>

          <Button
            variant="text"
            size="small"
            onClick={handleLimpiarFiltros}
          >
            Limpiar filtros
          </Button>
        </Box>
      </Card>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
          <CircularProgress />
        </Box>
      ) : solicitudesConDias.length === 0 ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 8, gap: 2 }}>
          <Typography color="text.secondary">
            No hay solicitudes que coincidan con los filtros.
          </Typography>
        </Box>
      ) : (
        <Card sx={{ border: '1px solid #DCDEE0', borderRadius: 2, overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
          <DataGrid
            rows={solicitudesConDias}
            columns={columns}
            autoHeight
            pageSizeOptions={[10, 25, 50]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            disableRowSelectionOnClick
            getRowClassName={(params) =>
              params.row.diasEnEstado > 7 && params.row.estado === 'PENDIENTE' ? 'fila-demorada' : ''
            }
            sx={{
              border: 'none',
              '& .MuiDataGrid-columnHeaders': {
                bgcolor: '#F0F0F0',
                fontWeight: 700,
                fontSize: 12,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                color: '#454545',
                borderBottom: '1px solid #E6E6E6',
              },
              '& .MuiDataGrid-row': {
                '&:hover': {
                  bgcolor: '#F9F9F9',
                },
              },
              '& .MuiDataGrid-cell': {
                borderBottom: '1px solid #E6E6E6',
              },
              '& .fila-demorada': {
                bgcolor: 'rgba(255,190,46,0.12)',
              },
            }}
          />
        </Card>
      )}

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
