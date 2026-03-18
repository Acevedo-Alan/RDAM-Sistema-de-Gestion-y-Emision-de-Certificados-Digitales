import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, CircularProgress,
} from '@mui/material';

export default function RechazoModal({ open, onClose, onConfirm, isLoading }) {
  const [motivo, setMotivo] = useState('');

  const handleConfirm = () => {
    if (motivo.trim()) {
      onConfirm(motivo);
      setMotivo('');
    }
  };

  const handleClose = () => {
    if (!isLoading) {
      setMotivo('');
      onClose();
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Rechazar solicitud</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        <TextField
          fullWidth
          multiline
          rows={4}
          label="Motivo del rechazo"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
          disabled={isLoading}
          placeholder="Ingresa el motivo por el cual se rechaza la solicitud..."
        />
      </DialogContent>
      <DialogActions sx={{ p: 2 }}>
        <Button onClick={handleClose} disabled={isLoading}>
          Cancelar
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          sx={{ bgcolor: '#D54309', '&:hover': { bgcolor: '#842029' } }}
          disabled={!motivo.trim() || isLoading}
        >
          {isLoading ? <CircularProgress size={20} sx={{ mr: 1 }} /> : null}
          Confirmar rechazo
        </Button>
      </DialogActions>
    </Dialog>
  );
}
