import React, { useEffect, useState, useCallback } from 'react';
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
} from '@mui/material';
import client from '../api/client';

const WebhookSettings = () => {
  const [url, setUrl] = useState('');
  const [active, setActive] = useState(true);
  const [saving, setSaving] = useState(false);
  const [recent, setRecent] = useState([]);
  const [dlq, setDlq] = useState([]);
  const [loadingLists, setLoadingLists] = useState(false);

  const loadConfig = useCallback(async () => {
    try {
      const res = await client.get('/webhooks/config');
      const cfg = res.data || {};
      setUrl(cfg.url || '');
      setActive(cfg.isActive !== false);
    } catch {}
  }, []);

  const saveConfig = async () => {
    setSaving(true);
    try {
      await client.post('/webhooks/config', { url, isActive: active });
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
            />
            <FormControlLabel
              control={<Switch checked={active} onChange={(e) => setActive(e.target.checked)} />}
              label={active ? 'Active' : 'Inactive'}
            />
            <Button variant="contained" disabled={saving} onClick={saveConfig}>Save</Button>
          </Stack>
        </CardContent>
      </Card>

      <GridLikeTwoCols>
        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 1 }}>Recent Deliveries</Typography>
            {renderTable(recent)}
          </CardContent>
        </Card>
        <Card>
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
              <Typography variant="h6">Dead-letter Queue</Typography>
              <Button size="small" onClick={resendAllDlq} disabled={loadingLists || dlq.length === 0}>Resend all</Button>
            </Stack>
            {renderTable(dlq)}
          </CardContent>
        </Card>
      </GridLikeTwoCols>
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


