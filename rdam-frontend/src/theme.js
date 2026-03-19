import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#005EA2',
      dark: '#0F4A7C',
    },
    success: { main: '#00A91C' },
    warning: { main: '#FFBE2E' },
    error: { main: '#D54309' },
    text: {
      primary: '#1B1B1B',
      secondary: '#71767A',
    },
    divider: '#DCDEE0',
    background: {
      default: '#F9F9F9',
      paper: '#FFFFFF',
    },
  },
  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily: '"Public Sans", sans-serif',
    button: {
      textTransform: 'none',
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
  },
});

export default theme;
