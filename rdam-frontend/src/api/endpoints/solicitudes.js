import api from '../axios';

export const getMisSolicitudes = () =>
  api.get('/api/solicitudes/mis-solicitudes');

export const getSolicitudes = (params) =>
  api.get('/api/solicitudes', { params });

export const getSolicitud = (id) =>
  api.get(`/api/solicitudes/${id}`);

export const getHistorialSolicitud = (id) =>
  api.get(`/api/solicitudes/${id}/historial`);

export const getHistorial = (params) =>
  api.get('/api/solicitudes/historial', { params });

export const crearSolicitud = (data) =>
  api.post('/api/solicitudes', data);

export const cancelarSolicitud = (id) =>
  api.patch(`/api/solicitudes/${id}/cancelar`);

export const getPagoDatos = (id) =>
  api.get(`/api/solicitudes/${id}/pago-datos`);

export const actualizarSolicitud = (id, data) =>
  api.put(`/api/solicitudes/${id}`, data);

export const tomarSolicitud = (id) =>
  api.patch(`/api/solicitudes/${id}/tomar`);

export const aprobarSolicitud = (id) =>
  api.patch(`/api/solicitudes/${id}/aprobar`);

export const rechazarSolicitud = (id, data) =>
  api.patch(`/api/solicitudes/${id}/rechazar`, data);

export const reasignarSolicitud = (id, data) =>
  api.patch(`/api/solicitudes/${id}/reasignar`, data);

export const getBandeja = () =>
  api.get('/api/solicitudes/bandeja');

export const emitirCertificado = (id) =>
  api.post(`/api/certificados/emitir`, { solicitudId: id });

export const getInternos = () =>
  api.get('/api/admin/usuarios', { params: { rol: 'interno' } });
