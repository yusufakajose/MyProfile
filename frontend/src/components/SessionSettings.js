import React, { useEffect, useState } from 'react';
import { Box, Card, CardContent, Typography, Alert, Button, Stack, Chip } from '@mui/material';
import dayjs from 'dayjs';
import client from '../api/client';

const SessionSettings = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await client.get('/sessions');
      setSessions(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      setError('Failed to load sessions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const revoke = async (id) => {
    setError('');
    setSuccess('');
    try {
      await client.delete(`/sessions/${id}`);
      setSuccess('Session revoked');
      await load();
    } catch (e) {
      setError('Failed to revoke session');
    }
  };

  return (
    <Box maxWidth={900} mx="auto">
      <Typography variant="h5" gutterBottom>Sessions</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
      <Card>
        <CardContent>
          {sessions.length === 0 ? (
            <Typography color="text.secondary">No active sessions.</Typography>
          ) : (
            <Stack spacing={1.5}>
              {sessions.map((s) => (
                <Stack key={s.id} direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ xs: 'flex-start', sm: 'center' }} justifyContent="space-between" sx={{ p: 1, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
                  <Box sx={{ minWidth: 240 }}>
                    <Typography variant="body2"><strong>IP:</strong> {s.ip || '—'}</Typography>
                    <Typography variant="body2" sx={{ wordBreak: 'break-word' }}><strong>Agent:</strong> {s.userAgent || '—'}</Typography>
                  </Box>
                  <Box sx={{ minWidth: 240 }}>
                    <Typography variant="body2"><strong>Created:</strong> {s.createdAt ? dayjs(s.createdAt).format('YYYY-MM-DD HH:mm') : '—'}</Typography>
                    <Typography variant="body2"><strong>Last used:</strong> {s.lastUsedAt ? dayjs(s.lastUsedAt).format('YYYY-MM-DD HH:mm') : '—'}</Typography>
                  </Box>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Chip label={s.revoked ? 'Revoked' : 'Active'} color={s.revoked ? 'default' : 'success'} size="small" />
                    <Button variant="outlined" color="error" size="small" onClick={() => revoke(s.id)} disabled={loading || s.revoked}>Revoke</Button>
                  </Stack>
                </Stack>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default SessionSettings;


