import React from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Link from '@mui/material/Link';
import { useAuth } from '../context/AuthContext';

const Header = () => {
  const navigate = useNavigate();
  const { token, user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/member-login');
  };

  return (
    <AppBar
      position="sticky"
      color="default"
      elevation={2}
      sx={{
        borderBottom: '1px solid',
        borderColor: 'divider',
        backgroundColor: 'background.paper',
        backdropFilter: 'saturate(180%) blur(6px)'
      }}
    >
      <Toolbar sx={{ py: 1.25 }}>
        <Box sx={{ flexGrow: 1 }}>
          <Typography
            variant="h6"
            component={RouterLink}
            to="/"
            sx={{ textDecoration: 'none', color: 'text.primary', fontWeight: 800, letterSpacing: 0.3 }}
          >
            LinkGrove
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} alignItems="center">
          {token ? (
            <>
              <Link
                component={RouterLink}
                to="/analytics"
                underline="none"
                sx={{
                  color: 'text.secondary', fontWeight: 600, px: 1.25, py: 0.5, borderRadius: 1,
                  '&:hover': { backgroundColor: 'action.hover' }
                }}
              >
                Analytics
              </Link>
              <Link
                component={RouterLink}
                to="/links"
                underline="none"
                sx={{
                  color: 'text.secondary', fontWeight: 600, px: 1.25, py: 0.5, borderRadius: 1,
                  '&:hover': { backgroundColor: 'action.hover' }
                }}
              >
                Links
              </Link>
              <Link
                component={RouterLink}
                to="/settings/profile"
                underline="none"
                sx={{
                  color: 'text.secondary', fontWeight: 600, px: 1.25, py: 0.5, borderRadius: 1,
                  '&:hover': { backgroundColor: 'action.hover' }
                }}
              >
                Profile
              </Link>
              <Link
                component={RouterLink}
                to="/settings/webhooks"
                underline="none"
                sx={{
                  color: 'text.secondary', fontWeight: 600, px: 1.25, py: 0.5, borderRadius: 1,
                  '&:hover': { backgroundColor: 'action.hover' }
                }}
              >
                Webhooks
              </Link>
              <Typography variant="body2" sx={{ ml: 1, mr: 1, color: 'text.secondary' }}>{user?.username || ''}</Typography>
              <Button variant="outlined" size="small" onClick={handleLogout}>Logout</Button>
            </>
          ) : (
            <>
              <Button component={RouterLink} to="/member-login" variant="outlined" size="small">Login</Button>
              <Button component={RouterLink} to="/register" variant="contained" size="small">Register</Button>
            </>
          )}
        </Stack>
      </Toolbar>
    </AppBar>
  );
};

export default Header;


