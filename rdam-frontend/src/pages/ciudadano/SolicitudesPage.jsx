import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button, CircularProgress, Alert, Box, Card, Typography } from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import dayjs from 'dayjs';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import { getMisSolicitudes, getSolicitudes } from '../../api/endpoints/solicitudes';

export default function SolicitudesPage() {
  const navigate = useNavigate();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['mis-solicitudes'],
    queryFn: async () => {
      try {
        const res = await getMisSolicitudes();
        return res.data;
      } catch (err) {
        if (err.response?.status === 404) {
          const res = await getSolicitudes();
          return res.data;
        }
        throw err;
      }
    },
  });

  const solicitudes = Array.isArray(data) ? data : data?.content ?? [];

  const columns = [
    {
      field: 'numeroTramite',
      headerName: 'N° Tramite',
      width: 200,
      valueGetter: (value, row) => row.numeroTramite ?? row.id,
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
      flex: 1,
      valueGetter: (value, row) => row.circunscripcion?.nombre ?? row.circunscripcionNombre ?? value ?? '',
    },
    {
      field: 'fechaCreacion',
      headerName: 'Fecha',
      width: 160,
      valueGetter: (value, row) => row.fechaCreacion || row.createdAt || row.fecha,
      valueFormatter: (value) => value ? dayjs(value).format('DD/MM/YYYY') : '',
    },
    {
      field: 'estado',
      headerName: 'Estado',
      minWidth: 180,
      renderCell: (params) => <StatusBadge estado={params.value} />,
    },
    {
      field: 'acciones',
      headerName: 'Acciones',
      width: 180,
      sortable: false,
      filterable: false,
      renderCell: (params) => {
        const estado = params.row.estado;
        return (
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', height: '100%' }}>
            <Button
              size="small"
              variant="outlined"
              onClick={() => navigate(`/ciudadano/solicitudes/${params.row.id}`)}
            >
              Ver
            </Button>

            {estado === 'PUBLICADO' && (
              <Button size="small" variant="outlined" color="success">
                Descargar
              </Button>
            )}
          </Box>
        );
      },
    },
  ];

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError) {
    return <Alert severity="error">Error al cargar las solicitudes</Alert>;
  }

  if (solicitudes.length === 0) {
    return (
      <>
        <PageHeader title="Mis Solicitudes">
          <Button variant="contained" onClick={() => navigate('/ciudadano/nueva')}>
            Nueva Solicitud
          </Button>
        </PageHeader>
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 8, gap: 2 }}>
          <Typography color="text.secondary">
            No tenes solicitudes. Crea tu primera solicitud.
          </Typography>
          <Button variant="contained" onClick={() => navigate('/ciudadano/nueva')}>
            Crear solicitud
          </Button>
        </Box>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Mis Solicitudes">
        <Button variant="contained" sx={{ bgcolor: '#005EA2', '&:hover': { bgcolor: '#0F4A7C' } }} onClick={() => navigate('/ciudadano/nueva')}>
          Nueva Solicitud
        </Button>
      </PageHeader>
      <Card sx={{ border: '1px solid #DCDEE0', borderRadius: 2, overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
        <DataGrid
          rows={solicitudes}
          columns={columns}
          autoHeight
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
          disableRowSelectionOnClick
          localeText={{
            MuiTablePagination: { labelRowsPerPage: 'Filas por pagina' },
            columnMenuHideColumn: 'Ocultar columna',
            columnMenuManageColumns: 'Administrar columnas',
            noRowsLabel: 'No hay solicitudes',
            footerRowSelected: (count) => `${count} fila(s) seleccionada(s)`,
          }}
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
            '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 700, fontSize: 12, letterSpacing: '0.5px' },
            '& .MuiDataGrid-row': { fontSize: 14 },
            '& .MuiDataGrid-row:hover': {
              bgcolor: '#F9F9F9',
            },
            '& .MuiDataGrid-cell': {
              borderBottom: '1px solid #E6E6E6',
            },
            '& .MuiDataGrid-footerContainer': {
              borderTop: '1px solid #E6E6E6',
              bgcolor: '#F9F9F9',
            },
          }}
        />
      </Card>
    </>
  );
}
