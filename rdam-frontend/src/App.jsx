import { Snackbar, Alert } from '@mui/material';
import { useAuth } from './hooks/useAuth';
import AppRouter from './router';

export default function App({ toggleMode, mode }) {
  const { sessionExpired, clearSessionExpired } = useAuth();

  return (
    <>
      <AppRouter toggleMode={toggleMode} mode={mode} />
      <Snackbar
        open={sessionExpired}
        autoHideDuration={6000}
        onClose={clearSessionExpired}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity="warning" onClose={clearSessionExpired} variant="filled">
          Tu sesión expiró. Por favor volvé a ingresar.
        </Alert>
      </Snackbar>
    </>
  );
}
