import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Menu,
  MenuItem,
  Tooltip,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  TrendingDown as SavingsIcon,
  Hub as VisualizerIcon,
  Brightness4 as DarkModeIcon,
  Brightness7 as LightModeIcon,
  ExitToApp as LogoutIcon,
  History as HistoryIcon,
  Psychology as TemporalIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';

const drawerWidth = 260;

interface LayoutProps {
  mode: 'light' | 'dark';
  toggleTheme: () => void;
}

export const Layout: React.FC<LayoutProps> = ({ mode, toggleTheme }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    handleProfileMenuClose();
    logout();
    navigate('/login');
  };

  const menuItems = [
    { text: 'Dashboard', icon: <DashboardIcon />, path: '/' },
    { text: 'Recommendations', icon: <SavingsIcon />, path: '/recommendations' },
    { text: 'Cluster Visualizer', icon: <VisualizerIcon />, path: '/visualizer' },
    { text: 'Audit Logs', icon: <HistoryIcon />, path: '/audit-logs' },
    { text: 'Temporal Profiles', icon: <TemporalIcon />, path: '/temporal-profiles' },
  ];

  const drawer = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, py: 2 }}>
        <Box
          component="span"
          sx={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontWeight: 800,
            fontSize: '1.1rem',
          }}
        >
          C
        </Box>
        <Typography variant="h6" noWrap sx={{ fontWeight: 700, letterSpacing: '-0.02em', background: 'linear-gradient(to right, #6366f1, #a855f7)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          Cost Optimizer
        </Typography>
      </Toolbar>
      <Divider />
      <List sx={{ px: 1.5, py: 2, flexGrow: 1 }}>
        {menuItems.map((item) => {
          const active = location.pathname === item.path;
          return (
            <ListItem key={item.text} disablePadding sx={{ mb: 0.8 }}>
              <ListItemButton
                onClick={() => navigate(item.path)}
                sx={{
                  borderRadius: '10px',
                  bgcolor: active ? 'primary.main' : 'transparent',
                  color: active ? '#ffffff' : 'text.primary',
                  '&:hover': {
                    bgcolor: active ? 'primary.dark' : 'action.hover',
                  },
                }}
              >
                <ListItemIcon sx={{ color: active ? '#ffffff' : 'text.secondary', minWidth: 40 }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={
                  <Typography sx={{ fontSize: '0.9rem', fontWeight: active ? 600 : 500 }}>
                    {item.text}
                  </Typography>
                } />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
      <Divider />
      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
          {user?.username?.charAt(0).toUpperCase() || 'A'}
        </Avatar>
        <Box sx={{ overflow: 'hidden' }}>
          <Typography variant="subtitle1" noWrap sx={{ fontWeight: 600 }}>
            {user?.username}
          </Typography>
          <Typography variant="body2" color="text.secondary" noWrap sx={{ fontSize: '0.75rem' }}>
            {user?.email}
          </Typography>
        </Box>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
          boxShadow: 'none',
          borderBottom: 1,
          borderColor: 'divider',
          bgcolor: 'background.paper',
          color: 'text.primary',
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 600 }}>
            {menuItems.find((item) => item.path === location.pathname)?.text || 'Cost Optimizer'}
          </Typography>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Tooltip title="Toggle light/dark mode">
              <IconButton onClick={toggleTheme} color="inherit">
                {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
              </IconButton>
            </Tooltip>
            <Tooltip title="User Profile">
              <IconButton onClick={handleProfileMenuOpen} color="inherit" sx={{ p: 0.5 }}>
                <Avatar sx={{ bgcolor: 'secondary.main', width: 32, height: 32 }}>
                  {user?.username?.charAt(0).toUpperCase() || 'A'}
                </Avatar>
              </IconButton>
            </Tooltip>
            <Menu
              anchorEl={anchorEl}
              open={Boolean(anchorEl)}
              onClose={handleProfileMenuClose}
              transformOrigin={{ horizontal: 'right', vertical: 'top' }}
              anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
            >
              <MenuItem disabled>Role: {user?.role?.replace('ROLE_', '')}</MenuItem>
              <Divider />
              <MenuItem onClick={handleLogout}>
                <ListItemIcon>
                  <LogoutIcon fontSize="small" />
                </ListItemIcon>
                Logout
              </MenuItem>
            </Menu>
          </Box>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
        aria-label="mailbox folders"
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, borderRight: 1, borderColor: 'divider' },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, borderRight: 1, borderColor: 'divider' },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
};
