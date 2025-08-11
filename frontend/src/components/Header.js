import React from 'react';
import { AppBar, Toolbar, Typography, Box, Button } from '@mui/material';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import { Analytics as AnalyticsIcon } from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';

const Header = () => {
  const { user, token, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const handleLogout = () => { logout(); navigate('/member-login'); };
  const isAuthenticated = Boolean(token);
  const isAuthPage = location.pathname === '/member-login' || location.pathname === '/register';
  return (
    <AppBar position="static" elevation={1}>
      <Toolbar>
        <AnalyticsIcon sx={{ mr: 2 }} />
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          LinkGrove Analytics Dashboard
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isAuthenticated && !isAuthPage && (
            <>
              <Button color="inherit" component={RouterLink} to="/analytics">Analytics</Button>
              <Button color="inherit" component={RouterLink} to="/links">Links</Button>
              <Button color="inherit" component={RouterLink} to="/settings/profile">Settings</Button>
              <Button color="inherit" component={RouterLink} to="/settings/webhooks">Webhooks</Button>
              {user?.username && (
                <Button color="inherit" component={RouterLink} to={`/u/${user.username}`}>Profile</Button>
              )}
              <Button color="inherit" onClick={handleLogout}>Logout</Button>
            </>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Header;
