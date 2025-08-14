import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  FormControlLabel,
  Switch,
  Button,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Snackbar,
  Alert,
  LinearProgress,
} from '@mui/material';
import client from '../api/client';

const WebhookSettings = () => {
  const [url, setUrl] = useState('');
  const [active, setActive] = useState(true);
  const [saving, setSaving] = useState(false);
  const [recent, setRecent] = useState([]);
  const [dlq, setDlq] = useState([]);
  const [loadingLists, setLoadingLists] = useState(false);
  const [notice, setNotice] = useState({ open: false, message: '', severity: 'success' });

  const isValidUrl = useMemo(() => {
    if (!url) return true; // allow empty to disable webhooks
    try {
      const u = new URL(url);
      return u.protocol === 'http:' || u.protocol === 'https:';
    } catch {
      return false;
    }
  }, [url]);

  const loadConfig = useCallback(async () => {
    try {
      const res = await client.get('/webhooks/config');
      const cfg = res.data || {};
      setUrl(cfg.url || '');
      setActive(cfg.isActive !== false);
    } catch {}
  }, []);

  const saveConfig = async () => {
    if (!isValidUrl) {
      setNotice({ open: true, message: 'Please enter a valid URL', severity: 'error' });
      return;
    }
    setSaving(true);
    try {
      await client.post('/webhooks/config', { url, isActive: active });
      setNotice({ open: true, message: 'Webhook settings saved', severity: 'success' });
    } catch (e) {
      setNotice({ open: true, message: e?.response?.data?.message || 'Failed to save settings', severity: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const loadDeliveries = useCallback(async () => {
    setLoadingLists(true);
    try {
      const [r1, r2] = await Promise.all([
        client.get('/webhooks/deliveries'),
        client.get('/webhooks/deliveries/dlq'),
      ]);
      setRecent(Array.isArray(r1.data) ? r1.data : []);
      setDlq(Array.isArray(r2.data) ? r2.data : []);
    } finally {
      setLoadingLists(false);
    }
  }, []);

  const resend = async (id) => {
    await client.post(`/webhooks/deliveries/${id}/resend`);
    await loadDeliveries();
  };

  const resendAllDlq = async () => {
    await client.post('/webhooks/deliveries/resend-all-dlq');
    await loadDeliveries();
  };

  useEffect(() => {
    loadConfig();
    loadDeliveries();
  }, [loadConfig, loadDeliveries]);

  const renderTable = (rows) => (
    (rows && rows.length > 0) ? (
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>When</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Error</TableCell>
            <TableCell>Event</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {(rows || []).map((r) => (
            <TableRow key={r.id} hover>
              <TableCell>{r.createdAt ? new Date(r.createdAt).toLocaleString() : ''}</TableCell>
              <TableCell>{r.statusCode}</TableCell>
              <TableCell>{r.errorMessage || ''}</TableCell>
              <TableCell>{r.eventType}</TableCell>
              <TableCell align="right">
                <Button size="small" onClick={() => resend(r.id)}>Resend</Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    ) : (
      <Typography variant="body2" color="text.secondary">No deliveries yet</Typography>
    )
  );

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Webhooks</Typography>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
            <TextField
              label="Webhook URL"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              sx={{ flex: 1 }}
              placeholder="https://example.com/webhooks/link-click"
              error={!isValidUrl}
              helperText={!isValidUrl ? 'Enter a valid http(s) URL or leave empty to disable' : ' '}
            />
            <FormControlLabel
              control={<Switch checked={active} onChange={(e) => setActive(e.target.checked)} />}
              label={active ? 'Active' : 'Inactive'}
            />
            <Button variant="contained" disabled={saving || !isValidUrl} onClick={saveConfig}>Save</Button>
          </Stack>
        </CardContent>
      </Card>

      <GridLikeTwoCols>
        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 1 }}>Recent Deliveries</Typography>
            {loadingLists && <LinearProgress sx={{ mb: 1 }} />}
            {renderTable(recent)}
          </CardContent>
        </Card>
        <Card>
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
              <Typography variant="h6">Dead-letter Queue</Typography>
              <Button size="small" onClick={resendAllDlq} disabled={loadingLists || dlq.length === 0}>Resend all</Button>
            </Stack>
            {loadingLists && <LinearProgress sx={{ mb: 1 }} />}
            {renderTable(dlq)}
          </CardContent>
        </Card>
      </GridLikeTwoCols>

      <Snackbar
        open={notice.open}
        autoHideDuration={2500}
        onClose={() => setNotice({ open: false, message: '', severity: 'success' })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={notice.severity} sx={{ width: '100%' }}>{notice.message}</Alert>
      </Snackbar>
    </Box>
  );
};

// Simple responsive two-column layout using Box
const GridLikeTwoCols = ({ children }) => (
  <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
    {children}
  </Box>
);

export default WebhookSettings;


