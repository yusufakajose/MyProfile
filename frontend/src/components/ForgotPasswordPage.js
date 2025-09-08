import React, { useState } from 'react';
import { Box, Card, CardContent, TextField, Typography, Alert, Button } from '@mui/material';
import client from '../api/client';

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      await client.post('/auth/initiate-password-reset', { email });
      setSuccess('If that email exists, a reset link has been sent.');
      setEmail('');
    } catch (e) {
      setError('Failed to initiate password reset');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
      <Card sx={{ maxWidth: 480, width: '100%' }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>Forgot Password</Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
          <Box component="form" onSubmit={submit}>
            <TextField fullWidth type="email" label="Email" margin="normal" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
            <Button type="submit" variant="contained" disabled={loading || !email} sx={{ mt: 2 }}>Send Reset Link</Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ForgotPasswordPage;


