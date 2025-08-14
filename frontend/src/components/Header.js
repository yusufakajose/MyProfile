import React from 'react';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Link from '@mui/material/Link';
import IconButton from '@mui/material/IconButton';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import ListItemIcon from '@mui/material/ListItemIcon';
import Divider from '@mui/material/Divider';
import Container from '@mui/material/Container';
import Avatar from '@mui/material/Avatar';
import Tooltip from '@mui/material/Tooltip';
import { useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import MenuIcon from '@mui/icons-material/Menu';
import QueryStatsIcon from '@mui/icons-material/QueryStats';
import { Link as LinkIcon } from '@mui/icons-material';
import PersonIcon from '@mui/icons-material/Person';
import WebhookIcon from '@mui/icons-material/Webhook';
import AddLinkIcon from '@mui/icons-material/AddLink';
import LogoutIcon from '@mui/icons-material/Logout';
import LoginIcon from '@mui/icons-material/Login';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import { useAuth } from '../context/AuthContext';
import { ColorModeContext } from '../theme/ColorModeContext';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';

const Header = () => {
  const navigate = useNavigate();
  const { token, user, logout } = useAuth();
  const location = useLocation();
  const theme = useTheme();
  const isSmall = useMediaQuery(theme.breakpoints.down('sm'));
  const [drawerOpen, setDrawerOpen] = React.useState(false);
  const colorMode = React.useContext(ColorModeContext);
  const [userMenuAnchor, setUserMenuAnchor] = React.useState(null);
  const openUserMenu = (e) => setUserMenuAnchor(e.currentTarget);
  const closeUserMenu = () => setUserMenuAnchor(null);

  const handleLogout = () => {
    logout();
    navigate('/member-login');
  };

  const openDrawer = () => setDrawerOpen(true);
  const closeDrawer = () => setDrawerOpen(false);

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(`${path}/`);

  const navItems = [
    { to: '/analytics', label: 'Analytics', icon: <QueryStatsIcon fontSize="small" /> },
    { to: '/links', label: 'Links', icon: <LinkIcon fontSize="small" /> },
    { to: '/settings/profile', label: 'Profile', icon: <PersonIcon fontSize="small" /> },
    { to: '/settings/webhooks', label: 'Webhooks', icon: <WebhookIcon fontSize="small" /> }
  ];

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
      <Container maxWidth="xl">
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
              {isSmall ? (
                <>
                  <Tooltip title="Menu">
                    <IconButton
                      aria-label="open navigation menu"
                      onClick={openDrawer}
                      size="small"
                      sx={{ mr: 1 }}
                    >
                      <MenuIcon />
                    </IconButton>
                  </Tooltip>
                  <Drawer anchor="right" open={drawerOpen} onClose={closeDrawer} PaperProps={{ sx: { width: '86vw', maxWidth: 360 } }}>
                    <Box sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ width: 40, height: 40, bgcolor: 'primary.main' }}>
                        {(user?.username || 'U').slice(0, 1).toUpperCase()}
                      </Avatar>
                      <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{user?.username || ''}</Typography>
                    </Box>
                    <Divider />
                    <List>
                      {navItems.map((item) => (
                        <ListItemButton
                          key={item.to}
                          component={RouterLink}
                          to={item.to}
                          selected={isActive(item.to)}
                          onClick={closeDrawer}
                          sx={{ py: 1.25 }}
                        >
                          <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
                          <ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: 700 }} />
                        </ListItemButton>
                      ))}
                      <Divider sx={{ my: 1 }} />
                      <ListItemButton component={RouterLink} to="/links" onClick={closeDrawer} sx={{ py: 1.25 }}>
                        <ListItemIcon sx={{ minWidth: 36 }}><AddLinkIcon fontSize="small" /></ListItemIcon>
                        <ListItemText primary="Create Link" primaryTypographyProps={{ fontWeight: 700 }} />
                      </ListItemButton>
                      <ListItemButton onClick={() => { closeDrawer(); handleLogout(); }} sx={{ py: 1.25 }}>
                        <ListItemIcon sx={{ minWidth: 36 }}><LogoutIcon fontSize="small" /></ListItemIcon>
                        <ListItemText primary="Logout" primaryTypographyProps={{ fontWeight: 700 }} />
                      </ListItemButton>
                    </List>
                  </Drawer>
                </>
              ) : (
                <>
                  {navItems.map((item) => (
                    <Link
                      key={item.to}
                      component={RouterLink}
                      to={item.to}
                      underline="none"
                      aria-current={isActive(item.to) ? 'page' : undefined}
                      sx={{
                        color: isActive(item.to) ? 'text.primary' : 'text.secondary',
                        fontWeight: 700,
                        px: 1.25,
                        py: 0.5,
                        borderRadius: 1,
                        '&:hover': { backgroundColor: 'action.hover' },
                        ...(isActive(item.to) ? { backgroundColor: 'action.selected' } : {})
                      }}
                    >
                      {item.label}
                    </Link>
                  ))}
                  <Box sx={{ display: 'flex', alignItems: 'center', ml: 2, gap: 1 }}>
                    <Button component={RouterLink} to="/links" variant="contained" size="small">
                      Create Link
                    </Button>
                    <Tooltip title={`Switch to ${theme.palette.mode === 'light' ? 'dark' : 'light'} mode`}>
                      <IconButton onClick={colorMode.toggleColorMode} aria-label="toggle color mode">
                        {theme.palette.mode === 'light' ? <Brightness4Icon /> : <Brightness7Icon />}
                      </IconButton>
                    </Tooltip>
                    <IconButton onClick={openUserMenu} aria-label="open user menu">
                      <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
                        {(user?.username || 'U').slice(0, 1).toUpperCase()}
                      </Avatar>
                    </IconButton>
                    <Menu anchorEl={userMenuAnchor} open={Boolean(userMenuAnchor)} onClose={closeUserMenu} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} transformOrigin={{ vertical: 'top', horizontal: 'right' }}>
                      <MenuItem component={RouterLink} to="/settings/profile" onClick={closeUserMenu}>Profile</MenuItem>
                      <MenuItem onClick={() => { closeUserMenu(); handleLogout(); }}>
                        <ListItemIcon sx={{ minWidth: 28 }}><LogoutIcon fontSize="small" /></ListItemIcon>
                        Logout
                      </MenuItem>
                    </Menu>
                  </Box>
                </>
              )}
            </>
          ) : (
            <>
              {isSmall ? (
                <>
                  <Tooltip title="Menu">
                    <IconButton aria-label="open navigation menu" onClick={openDrawer} size="small" sx={{ mr: 1 }}>
                      <MenuIcon />
                    </IconButton>
                  </Tooltip>
                  <Drawer anchor="right" open={drawerOpen} onClose={closeDrawer} PaperProps={{ sx: { width: '86vw', maxWidth: 360 } }}>
                    <Box sx={{ p: 2 }}>
                      <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>Menu</Typography>
                    </Box>
                    <Divider />
                    <List>
                      <ListItemButton component={RouterLink} to="/member-login" onClick={closeDrawer} sx={{ py: 1.25 }}>
                        <ListItemIcon sx={{ minWidth: 36 }}><LoginIcon fontSize="small" /></ListItemIcon>
                        <ListItemText primary="Login" primaryTypographyProps={{ fontWeight: 700 }} />
                      </ListItemButton>
                      <ListItemButton component={RouterLink} to="/register" onClick={closeDrawer} sx={{ py: 1.25 }}>
                        <ListItemIcon sx={{ minWidth: 36 }}><PersonAddIcon fontSize="small" /></ListItemIcon>
                        <ListItemText primary="Register" primaryTypographyProps={{ fontWeight: 700 }} />
                      </ListItemButton>
                    </List>
                  </Drawer>
                </>
              ) : (
                <>
                  <Button component={RouterLink} to="/member-login" variant="outlined" size="small">Login</Button>
                  <Button component={RouterLink} to="/register" variant="contained" size="small">Register</Button>
                  <Tooltip title={`Switch to ${theme.palette.mode === 'light' ? 'dark' : 'light'} mode`}>
                    <IconButton onClick={colorMode.toggleColorMode} sx={{ ml: 1 }} aria-label="toggle color mode">
                      {theme.palette.mode === 'light' ? <Brightness4Icon /> : <Brightness7Icon />}
                    </IconButton>
                  </Tooltip>
                </>
              )}
            </>
          )}
        </Stack>
      </Toolbar>
      </Container>
    </AppBar>
  );
};

export default Header;


