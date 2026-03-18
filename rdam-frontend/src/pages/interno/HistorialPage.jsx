import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, TextField, CircularProgress,
  Typography,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { LocalizationProvider, DatePicker } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import { getHistorial } from '../../api/endpoints/solicitudes';

export default function HistorialPage() {
  const [filtros, setFiltros] = useState({
    search: '',
    desde: null,
    hasta: null,
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['historial-solicitudes', filtros],
    queryFn: () => {
      const params = {};
      if (filtros.search) params.search = filtros.search;
      if (filtros.desde) params.desde = filtros.desde.format('YYYY-MM-DD');
      if (filtros.hasta) params.hasta = filtros.hasta.format('YYYY-MM-DD');
      return getHistorial(params).then((r) => r.data);
    },
  });

  const historialItems = Array.isArray(data) ? data : data?.content ?? [];

  const handleLimpiarFiltros = () => {
    setFiltros({ search: '', desde: null, hasta: null });
  };

  const columns = [
    {
      field: 'id',
      headerName: 'N° Solicitud',
      width: 140,
      valueGetter: (value, row) => row.solicitudId ?? row.id ?? '',
    },
    {
      field: 'estadoAnterior',
      headerName: 'Estado anterior',
      width: 150,
    },
    {
      field: 'estadoNuevo',
      headerName: 'Estado nuevo',
      width: 150,
      renderCell: (params) => <StatusBadge estado={params.row.estado ?? params.row.estadoNuevo} />,
    },
    {
      field: 'usuario',
      headerName: 'Usuario',
      flex: 1,
    },
    {
      field: 'fecha',
      headerName: 'Fecha y hora',
      width: 180,
      valueFormatter: (value) => value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '',
    },
    {
      field: 'observacion',
      headerName: 'Observacion',
      flex: 1,
    },
  ];

  if (isError) {
    return <Alert severity="error">Error al cargar el historial</Alert>;
  }

  return (
    <>
      <PageHeader title="Historial de solicitudes" />

      <Card sx={{ border: '1px solid #DCDEE0', borderRadius: 2, p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <TextField
            size="small"
            label="Buscar por N° de solicitud"
            value={filtros.search}
            onChange={(e) => setFiltros({ ...filtros, search: e.target.value })}
            sx={{ width: 280 }}
          />

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
      ) : historialItems.length === 0 ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 8, gap: 2 }}>
          <Typography color="text.secondary">
            No hay registros de historial que coincidan con los filtros.
          </Typography>
        </Box>
      ) : (
        <Card sx={{ border: '1px solid #DCDEE0', borderRadius: 2, overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
          <DataGrid
            rows={historialItems}
            columns={columns}
            autoHeight
            pageSizeOptions={[10, 25, 50]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            disableRowSelectionOnClick
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
            }}
          />
        </Card>
      )}
    </>
  );
}
