import api from '../axios';

export const getUsuarios = (params) =>
  api.get('/admin/usuarios', { params });

export const getUsuario = (id) =>
  api.get(`/admin/usuarios/${id}`);

export const actualizarUsuario = (id, data) =>
  api.put(`/admin/usuarios/${id}`, data);

export const getReportes = (params) =>
  api.get('/admin/reportes', { params });
