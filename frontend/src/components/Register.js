import React, { useState } from 'react';
import { Box, Button, Card, CardContent, TextField, Typography, Alert, Link as MuiLink } from '@mui/material';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Link, useNavigate } from 'react-router-dom';

const Register = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await client.post('/auth/register', { username, email, password });
      login(res.data.token, { username: res.data.username, email: res.data.email });
      navigate('/analytics');
    } catch (err) {
      setError(err?.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
      <Card sx={{ maxWidth: 480, width: '100%' }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>Create Account</Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Box component="form" onSubmit={handleSubmit}>
            <TextField fullWidth label="Username" margin="normal" value={username} onChange={(e) => setUsername(e.target.value)} />
            <TextField fullWidth label="Email" type="email" margin="normal" value={email} onChange={(e) => setEmail(e.target.value)} />
            <TextField fullWidth label="Password" type="password" margin="normal" value={password} onChange={(e) => setPassword(e.target.value)} />
            <Button type="submit" variant="contained" color="primary" fullWidth disabled={loading} sx={{ mt: 2 }}>
              {loading ? 'Creating...' : 'Register'}
            </Button>
          </Box>
          <Typography variant="body2" sx={{ mt: 2 }}>
            Have an account? <MuiLink component={Link} to="/member-login">Sign in</MuiLink>
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default Register;


