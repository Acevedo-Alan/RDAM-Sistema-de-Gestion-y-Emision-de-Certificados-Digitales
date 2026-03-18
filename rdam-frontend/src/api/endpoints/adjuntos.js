import api from '../axios';

export const uploadAdjunto = (solicitudId, archivo) => {
  const formData = new FormData()
  formData.append('archivo', archivo)
  return api.post(`/api/solicitudes/${solicitudId}/adjuntos`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const downloadAdjunto = (id) =>
  `/api/adjuntos/${id}/download`;
