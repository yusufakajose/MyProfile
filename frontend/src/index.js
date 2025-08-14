import React, { useMemo, useState } from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import App from './App';
import { ColorModeContext } from './theme/ColorModeContext';

const getDesignTokens = (mode) => ({
  palette: {
    mode,
    primary: { main: '#2563eb' },
    secondary: { main: '#7c3aed' },
    ...(mode === 'light'
      ? { background: { default: '#f7f8fb', paper: '#ffffff' }, text: { primary: '#0f172a', secondary: '#475569' } }
      : { background: { default: '#0b1220', paper: '#121a2a' }, text: { primary: '#e6e8ee', secondary: '#9aa4b2' } }),
  },
  shape: { borderRadius: 14 },
  typography: {
    fontFamily: 'Inter, Roboto, Arial, sans-serif',
    h5: { fontWeight: 700 },
    h6: { fontWeight: 600 },
    button: { fontWeight: 700, textTransform: 'none' },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        html: { scrollBehavior: 'smooth' },
      },
    },
    MuiButton: {
      defaultProps: { size: 'large' },
      styleOverrides: {
        root: { borderRadius: 16, textTransform: 'none', paddingTop: 12, paddingBottom: 12 },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 18,
          boxShadow:
            '0 10px 30px rgba(2,6,23,0.08), 0 4px 10px rgba(2,6,23,0.06)',
        },
      },
    },
  },
});

const Root = () => {
  const [mode, setMode] = useState(() => {
    const saved = typeof localStorage !== 'undefined' ? localStorage.getItem('lg-theme') : null;
    return saved === 'dark' ? 'dark' : 'light';
  });
  const colorMode = useMemo(() => ({
    mode,
    toggleColorMode: () => setMode((prev) => {
      const next = prev === 'light' ? 'dark' : 'light';
      try { localStorage.setItem('lg-theme', next); } catch {}
      return next;
    }),
  }), [mode]);
  const theme = useMemo(() => createTheme(getDesignTokens(mode)), [mode]);
  return (
    <BrowserRouter>
      <ColorModeContext.Provider value={colorMode}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <App />
        </ThemeProvider>
      </ColorModeContext.Provider>
    </BrowserRouter>
  );
};

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <Root />
  </React.StrictMode>
);
