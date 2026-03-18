import api from '../axios';

export const getTiposCertificado = () =>
  api.get('/api/catalogos/tipos-certificado');

export const getCircunscripciones = () =>
  api.get('/api/catalogos/circunscripciones');

export const getCatalogos = () =>
  api.get('/api/catalogos');

export const getCatalogo = (id) =>
  api.get(`/api/catalogos/${id}`);

export const crearCatalogo = (data) =>
  api.post('/api/catalogos', data);

export const actualizarCatalogo = (id, data) =>
  api.put(`/api/catalogos/${id}`, data);

export const eliminarCatalogo = (id) =>
  api.delete(`/api/catalogos/${id}`);
