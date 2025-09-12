import React, { useState } from 'react';
import { Box, Card, CardContent, TextField, Typography, Alert, Button, InputAdornment, IconButton } from '@mui/material';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { useSearchParams } from 'react-router-dom';
import client from '../api/client';

const ResetPasswordPage = () => {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [password, setPassword] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const blockClipboard = (e) => {
    e.preventDefault();
  };
  const preventContextMenu = (e) => {
    e.preventDefault();
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      await client.post('/auth/reset-password', { token, newPassword: password });
      setSuccess('Password has been reset. You can log in now.');
      setPassword('');
    } catch (e) {
      setError('Failed to reset password. The link may be invalid or expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
      <Card sx={{ maxWidth: 480, width: '100%' }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>Reset Password</Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
          <Box component="form" onSubmit={submit}>
            <TextField fullWidth type={showPwd ? 'text' : 'password'} label="New Password" margin="normal" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton aria-label="toggle password visibility" onClick={() => setShowPwd((v) => !v)} edge="end">
                    {showPwd ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              )
            }} inputProps={{ onCopy: blockClipboard, onCut: blockClipboard, onPaste: blockClipboard, onContextMenu: preventContextMenu }} />
            <Button type="submit" variant="contained" disabled={loading || !password || !token} sx={{ mt: 2 }}>Set New Password</Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ResetPasswordPage;


