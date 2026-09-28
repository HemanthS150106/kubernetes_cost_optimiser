import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  InputAdornment,
  IconButton,
} from '@mui/material';
import { Visibility, VisibilityOff, CloudQueue } from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/api';

const MuiTextField = TextField as any;

export const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [isRegister, setIsRegister] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      if (isRegister) {
        await authService.register({ username, password, email });
        setSuccess('Registration successful! You can now log in.');
        setIsRegister(false);
        setPassword('');
      } else {
        const response = await authService.login({ username, password });
        login(response.token, {
          username: response.username,
          email: response.email,
          role: response.role,
        });
        navigate('/');
      }
    } catch (err: any) {
      setError(
        err.response?.data?.message || 
        err.response?.data?.error || 
        'An unexpected error occurred. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(circle at top left, #0f172a, #1e1b4b, #0f172a)',
        px: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 420, overflow: 'visible', position: 'relative' }}>
        <Box
          sx={{
            position: 'absolute',
            top: -40,
            left: '50%',
            transform: 'translateX(-50%)',
            width: 80,
            height: 80,
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 30px rgba(99, 102, 241, 0.4)',
            color: '#fff',
          }}
        >
          <CloudQueue sx={{ fontSize: '2.5rem' }} />
        </Box>
        <CardContent sx={{ pt: 7, pb: 4, px: 4 }}>
          <Typography variant="h5" align="center" sx={{ fontWeight: 700, mb: 1, letterSpacing: '-0.02em' }}>
            {isRegister ? 'Create Account' : 'Welcome Back'}
          </Typography>
          <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
            {isRegister ? 'Register your administrator dashboard credentials' : 'Sign in to access your cost optimizer dashboard'}
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2, borderRadius: '8px' }}>
              {error}
            </Alert>
          )}

          {success && (
            <Alert severity="success" sx={{ mb: 2, borderRadius: '8px' }}>
              {success}
            </Alert>
          )}

          <form onSubmit={handleSubmit}>
            <MuiTextField
              fullWidth
              label="Username"
              variant="outlined"
              margin="normal"
              value={username}
              onChange={(e: any) => setUsername(e.target.value)}
              required
              disabled={loading}
              sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
            />
            {isRegister && (
              <MuiTextField
                fullWidth
                label="Email Address"
                variant="outlined"
                margin="normal"
                type="email"
                value={email}
                onChange={(e: any) => setEmail(e.target.value)}
                required
                disabled={loading}
                sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
              />
            )}
            <MuiTextField
              fullWidth
              label="Password"
              variant="outlined"
              margin="normal"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e: any) => setPassword(e.target.value)}
              required
              disabled={loading}
              sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      aria-label="toggle password visibility"
                      onClick={() => setShowPassword(!showPassword)}
                      edge="end"
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ mt: 3, mb: 2, py: 1.5, fontSize: '0.95rem' }}
            >
              {loading ? (
                <CircularProgress size={24} color="inherit" />
              ) : isRegister ? (
                'Register Account'
              ) : (
                'Sign In'
              )}
            </Button>
          </form>

          <Button
            fullWidth
            variant="text"
            size="small"
            disabled={loading}
            onClick={() => {
              setIsRegister(!isRegister);
              setError(null);
              setSuccess(null);
            }}
            sx={{ mt: 1, color: 'text.secondary' }}
          >
            {isRegister ? 'Already have an account? Sign In' : "Don't have an account? Register"}
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};
