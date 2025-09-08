import React, { useEffect, useState } from 'react';
import { Box, Card, CardContent, Typography, Alert, CircularProgress, Button } from '@mui/material';
import { useSearchParams, Link as RouterLink } from 'react-router-dom';
import client from '../api/client';

const VerifyEmailPage = () => {
  const [params] = useSearchParams();
  const [status, setStatus] = useState('idle'); // idle|loading|success|error
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = params.get('token');
    if (!token) {
      setStatus('error');
      setMessage('Missing token');
      return;
    }
    (async () => {
      setStatus('loading');
      try {
        await client.post('/auth/verify-email', { token });
        setStatus('success');
        setMessage('Email verified successfully.');
      } catch (e) {
        setStatus('error');
        setMessage('Verification failed or token expired.');
      }
    })();
  }, [params]);

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
      <Card sx={{ maxWidth: 520, width: '100%' }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>Email Verification</Typography>
          {status === 'loading' && <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}><CircularProgress size={18} /><Typography>Verifying...</Typography></Box>}
          {status === 'success' && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}
          {status === 'error' && <Alert severity="error" sx={{ mb: 2 }}>{message}</Alert>}
          <Button component={RouterLink} to="/member-login" variant="contained">Go to Login</Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default VerifyEmailPage;


