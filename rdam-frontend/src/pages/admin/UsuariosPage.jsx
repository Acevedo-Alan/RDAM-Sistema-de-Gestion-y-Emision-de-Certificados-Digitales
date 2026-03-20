import { useState, useMemo } from 'react';
import {
  Box, Card, TextField, Button, IconButton, Chip, Tooltip,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Snackbar, Alert, Typography, InputAdornment,
  FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import {
  Search as SearchIcon,
  Edit as EditIcon,
  Block as BlockIcon,
  Delete as DeleteIcon,
  PersonAdd as PersonAddIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import PageHeader from '../../components/common/PageHeader';

const MOCK_USERS = [
  { id: 1, nombre: 'Ciudadano', apellido: 'Test', email: '20123456789', rol: 'ciudadano', estado: 'activo', createdAt: '2025-11-15' },
  { id: 2, nombre: 'Empleado', apellido: 'Interno', email: 'EMP001', rol: 'interno', estado: 'activo', createdAt: '2025-12-01' },
  { id: 3, nombre: 'Administrador', apellido: 'Sistema', email: 'ADMIN001', rol: 'admin', estado: 'activo', createdAt: '2026-01-10' },
];

const rolColors = {
  admin: { bg: '#FDECEA', color: '#D54309' },
  interno: { bg: '#E8F0FE', color: '#005EA2' },
  ciudadano: { bg: '#E6F4EA', color: '#00A91C' },
};

export default function UsuariosPage() {
  const [users, setUsers] = useState(MOCK_USERS);
  const [search, setSearch] = useState('');
  const [rolFilter, setRolFilter] = useState('Todos');
  const [estadoFilter, setEstadoFilter] = useState('Todos');
  const [confirmDialog, setConfirmDialog] = useState({ open: false, action: '', user: null });
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchSearch =
        !search ||
        `${u.nombre} ${u.apellido} ${u.email}`.toLowerCase().includes(search.toLowerCase());
      const matchRol = rolFilter === 'Todos' || u.rol === rolFilter;
      const matchEstado = estadoFilter === 'Todos' || u.estado === estadoFilter;
      return matchSearch && matchRol && matchEstado;
    });
  }, [users, search, rolFilter, estadoFilter]);

  const handleAction = () => {
    const { action, user } = confirmDialog;
    if (!user) return;

    if (action === 'delete') {
      setUsers((prev) => prev.filter((u) => u.id !== user.id));
      setSnackbar({ open: true, message: `Usuario ${user.email} eliminado`, severity: 'success' });
    } else if (action === 'block') {
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, estado: 'bloqueado' } : u)),
      );
      setSnackbar({ open: true, message: `Usuario ${user.email} bloqueado`, severity: 'warning' });
    } else if (action === 'unblock') {
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, estado: 'activo' } : u)),
      );
      setSnackbar({ open: true, message: `Usuario ${user.email} desbloqueado`, severity: 'success' });
    }
    setConfirmDialog({ open: false, action: '', user: null });
  };

  const dialogMessages = {
    delete: (u) => `¿Estás seguro de eliminar a ${u?.nombre} ${u?.apellido}?`,
    block: (u) => `¿Bloquear el acceso de ${u?.nombre} ${u?.apellido}?`,
    unblock: (u) => `¿Desbloquear a ${u?.nombre} ${u?.apellido}?`,
  };

  const columns = [
    {
      field: 'nombre',
      headerName: 'Nombre completo',
      flex: 1,
      minWidth: 180,
      valueGetter: (value, row) => `${row.nombre} ${row.apellido}`,
    },
    {
      field: 'email',
      headerName: 'Email',
      flex: 1,
      minWidth: 200,
    },
    {
      field: 'rol',
      headerName: 'Rol',
      width: 130,
      renderCell: (params) => {
        const rc = rolColors[params.value] || { bg: '#F0F0F0', color: '#454545' };
        return (
          <Chip
            label={params.value}
            size="small"
            sx={{
              bgcolor: rc.bg,
              color: rc.color,
              fontWeight: 600,
              fontSize: 12,
              borderRadius: 1,
            }}
          />
        );
      },
    },
    {
      field: 'estado',
      headerName: 'Estado',
      width: 130,
      renderCell: (params) => (
        <Chip
          label={params.value === 'activo' ? 'Activo' : 'Bloqueado'}
          size="small"
          sx={{
            bgcolor: params.value === 'activo' ? '#E6F4EA' : '#FDECEA',
            color: params.value === 'activo' ? '#00A91C' : '#D54309',
            fontWeight: 600,
            fontSize: 12,
            borderRadius: 1,
          }}
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Registrado',
      width: 120,
      valueFormatter: (value) => {
        if (!value) return '';
        const d = new Date(value);
        return d.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' });
      },
    },
    {
      field: 'acciones',
      headerName: 'Acciones',
      width: 150,
      sortable: false,
      filterable: false,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center', height: '100%' }}>
          <Tooltip title="Editar">
            <IconButton size="small" sx={{ color: '#005EA2' }}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {params.row.estado === 'activo' ? (
            <Tooltip title="Bloquear">
              <IconButton
                size="small"
                sx={{ color: '#FFBE2E' }}
                onClick={() => setConfirmDialog({ open: true, action: 'block', user: params.row })}
              >
                <BlockIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Tooltip title="Desbloquear">
              <IconButton
                size="small"
                sx={{ color: '#00A91C' }}
                onClick={() => setConfirmDialog({ open: true, action: 'unblock', user: params.row })}
              >
                <CheckCircleIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          <Tooltip title="Eliminar">
            <IconButton
              size="small"
              sx={{ color: '#D54309' }}
              onClick={() => setConfirmDialog({ open: true, action: 'delete', user: params.row })}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Gestión de usuarios" subtitle="Administra los usuarios registrados en el sistema">
        <Button variant="contained" startIcon={<PersonAddIcon />}>
          Nuevo usuario
        </Button>
      </PageHeader>

      {/* Filters */}
      <Card sx={{ border: '1px solid', borderColor: 'divider', p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            size="small"
            placeholder="Buscar por nombre o email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            sx={{ width: 300 }}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
                  </InputAdornment>
                ),
              },
            }}
          />
          <FormControl size="small" sx={{ width: 140 }}>
            <InputLabel>Rol</InputLabel>
            <Select value={rolFilter} onChange={(e) => setRolFilter(e.target.value)} label="Rol">
              <MenuItem value="Todos">Todos</MenuItem>
              <MenuItem value="admin">Admin</MenuItem>
              <MenuItem value="interno">Interno</MenuItem>
              <MenuItem value="ciudadano">Ciudadano</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ width: 140 }}>
            <InputLabel>Estado</InputLabel>
            <Select value={estadoFilter} onChange={(e) => setEstadoFilter(e.target.value)} label="Estado">
              <MenuItem value="Todos">Todos</MenuItem>
              <MenuItem value="activo">Activo</MenuItem>
              <MenuItem value="bloqueado">Bloqueado</MenuItem>
            </Select>
          </FormControl>
          {(search || rolFilter !== 'Todos' || estadoFilter !== 'Todos') && (
            <Button
              variant="text"
              size="small"
              onClick={() => { setSearch(''); setRolFilter('Todos'); setEstadoFilter('Todos'); }}
            >
              Limpiar filtros
            </Button>
          )}
        </Box>
      </Card>

      {/* Results count */}
      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1.5 }}>
        {filteredUsers.length} usuario{filteredUsers.length !== 1 ? 's' : ''} encontrado{filteredUsers.length !== 1 ? 's' : ''}
      </Typography>

      {/* Table */}
      <Card sx={{ border: '1px solid', borderColor: 'divider', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
        <DataGrid
          rows={filteredUsers}
          columns={columns}
          autoHeight
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
          disableRowSelectionOnClick
          sx={{
            border: 'none',
            '& .MuiDataGrid-columnHeaders': {
              bgcolor: 'action.selected',
              fontWeight: 700,
              fontSize: 12,
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
              color: 'text.secondary',
              borderBottom: '1px solid',
              borderColor: 'divider',
            },
            '& .MuiDataGrid-columnHeaderTitle': { color: 'text.primary', fontWeight: 700 },
            '& .MuiDataGrid-columnHeader': { color: 'text.primary' },
            '& .MuiDataGrid-row:hover': {
              bgcolor: 'action.hover',
            },
            '& .MuiDataGrid-cell': {
              borderBottom: '1px solid',
              borderColor: 'divider',
            },
          }}
        />
      </Card>

      {/* Confirm dialog */}
      <Dialog
        open={confirmDialog.open}
        onClose={() => setConfirmDialog({ open: false, action: '', user: null })}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 600 }}>Confirmar acción</DialogTitle>
        <DialogContent>
          <Typography>
            {confirmDialog.user && dialogMessages[confirmDialog.action]?.(confirmDialog.user)}
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setConfirmDialog({ open: false, action: '', user: null })}>
            Cancelar
          </Button>
          <Button
            variant="contained"
            color={confirmDialog.action === 'delete' ? 'error' : 'primary'}
            onClick={handleAction}
          >
            Confirmar
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
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
