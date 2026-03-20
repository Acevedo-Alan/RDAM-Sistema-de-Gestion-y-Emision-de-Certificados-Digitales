import { createTheme } from '@mui/material/styles';

export const getTheme = (mode) => createTheme({
  palette: {
    mode,
    ...(mode === 'light' ? {
      primary: { main: '#005EA2', dark: '#0F4A7C' },
      success: { main: '#00A91C' },
      warning: { main: '#FFBE2E' },
      error: { main: '#D54309' },
      background: { default: '#F9F9F9', paper: '#FFFFFF' },
      text: { primary: '#1B1B1B', secondary: '#71767A' },
      divider: '#DCDEE0',
    } : {
      primary: { main: '#4A9FD4', dark: '#2378C3' },
      success: { main: '#00A91C' },
      warning: { main: '#FFBE2E' },
      error: { main: '#D54309' },
      background: { default: '#121212', paper: '#1E1E1E' },
      text: { primary: '#E8E8E8', secondary: '#A0A0A0' },
      divider: '#333333',
    }),
  },
  typography: {
    fontFamily: '"Public Sans", sans-serif',
    button: {
      textTransform: 'none',
    },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { borderRadius: 8 },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: { borderRadius: 8 },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: { borderRadius: 8 },
      },
    },
  },
});
