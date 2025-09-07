import React, { useMemo, useState } from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import App from './App';
import LinearProgress from '@mui/material/LinearProgress';
import { subscribeLoading } from './api/loadingTracker';
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
        '.skip-link': {
          position: 'absolute',
          left: '-9999px',
          top: 'auto',
          width: '1px',
          height: '1px',
          overflow: 'hidden',
        },
        '.skip-link:focus, .skip-link:focus-visible': {
          position: 'fixed',
          left: 8,
          top: 8,
          width: 'auto',
          height: 'auto',
          overflow: 'visible',
          zIndex: 4000,
          padding: '8px 12px',
          borderRadius: 8,
          backgroundColor: mode === 'light' ? '#ffffff' : '#121a2a',
          color: mode === 'light' ? '#0f172a' : '#e6e8ee',
          boxShadow: '0 0 0 3px rgba(37,99,235,0.45)',
          textDecoration: 'none',
          border: '1px solid',
          borderColor: mode === 'light' ? '#cbd5e1' : '#334155',
        },
      },
    },
    MuiButton: {
      defaultProps: { size: 'large' },
      styleOverrides: {
        root: {
          borderRadius: 16,
          textTransform: 'none',
          paddingTop: 12,
          paddingBottom: 12,
          '&.Mui-focusVisible': { boxShadow: '0 0 0 3px rgba(37,99,235,0.45)' },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          '&.Mui-focusVisible': { boxShadow: '0 0 0 3px rgba(37,99,235,0.45)' },
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          '&.Mui-focusVisible': {
            outline: '2px solid rgba(37,99,235,0.75)',
            outlineOffset: 2,
            backgroundColor: 'rgba(37,99,235,0.08)',
          },
        },
      },
    },
    MuiLink: {
      styleOverrides: {
        root: {
          '&:focus-visible': { outline: '2px solid rgba(37,99,235,0.75)', outlineOffset: 2 },
        },
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
          <GlobalTopProgress />
          <App />
        </ThemeProvider>
      </ColorModeContext.Provider>
    </BrowserRouter>
  );
};

const GlobalTopProgress = () => {
  const [count, setCount] = React.useState(0);
  React.useEffect(() => subscribeLoading(setCount), []);
  if (count <= 0) return null;
  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, zIndex: 2000 }}>
      <LinearProgress />
    </div>
  );
};

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <Root />
  </React.StrictMode>
);
