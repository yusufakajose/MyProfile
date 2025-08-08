import React, { useEffect, useMemo, useState } from 'react';
import { Box, Button, Card, CardContent, Grid, IconButton, Stack, TextField, Typography, Switch, FormControlLabel, Tooltip, Pagination, InputAdornment } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import client from '../api/client';

const LinkManager = () => {
  const [links, setLinks] = useState([]);
  const [form, setForm] = useState({ title: '', url: '', description: '', alias: '' });
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null); // id being edited
  const [editForm, setEditForm] = useState({ title: '', url: '', description: '', alias: '' });
  const [dragIndex, setDragIndex] = useState(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(12);
  const [totalPages, setTotalPages] = useState(1);
  const [query, setQuery] = useState('');
  const [pendingQuery, setPendingQuery] = useState('');

  const redirectOrigin = useMemo(() => {
    const base = client.defaults?.baseURL || '';
    try {
      const parsed = new URL(base);
      return parsed.origin; // baseURL is .../api; origin gives protocol+host
    } catch {
      const origin = window.location.origin || '';
      return origin.replace(':3000', ':8080');
    }
  }, []);

  const canCreate = useMemo(() => {
    return form.title.trim() && /^https?:\/\//i.test(form.url.trim());
  }, [form]);

  const load = async (nextPage = page, nextQuery = query) => {
    const params = new URLSearchParams({ page: String(nextPage - 1), size: String(size) });
    if (nextQuery.trim()) params.set('q', nextQuery.trim());
    const res = await client.get(`/links?${params.toString()}`);
    const data = res.data;
    if (Array.isArray(data)) {
      setLinks(data);
      setTotalPages(1);
    } else {
      setLinks(data.content || []);
      setTotalPages((data.totalPages || 1));
    }
  };

  useEffect(() => { load(1, query); setPage(1); }, [query, size]);

  const create = async (e) => {
    e.preventDefault();
    if (!canCreate) return;
    setLoading(true);
    try {
      await client.post('/links', form);
      setForm({ title: '', url: '', description: '', alias: '' });
      await load();
    } finally {
      setLoading(false);
    }
  };

  const remove = async (id) => {
    await client.delete(`/links/${id}`);
    await load();
  };

  const beginEdit = (link) => {
    setEditing(link.id);
    setEditForm({ title: link.title || '', url: link.url || '', description: link.description || '', alias: link.alias || '' });
  };

  const saveEdit = async (id) => {
    await client.put(`/links/${id}`, editForm);
    setEditing(null);
    await load();
  };

  const toggleActive = async (link) => {
    const payload = {
      title: link.title || '',
      url: link.url || '',
      description: link.description || '',
      isActive: !link.isActive,
    };
    await client.put(`/links/${link.id}`, payload);
    await load();
  };

  const move = async (index, delta) => {
    const target = index + delta;
    if (target < 0 || target >= links.length) return;
    const reordered = [...links];
    const [moved] = reordered.splice(index, 1);
    reordered.splice(target, 0, moved);
    setLinks(reordered);
    await client.put('/links/reorder', reordered.map(l => l.id));
    await load();
  };

  const shortUrlFor = (id, alias) => alias ? `${redirectOrigin}/r/a/${encodeURIComponent(alias)}` : `${redirectOrigin}/r/${id}`;

  const copyShort = async (id, alias) => {
    const url = shortUrlFor(id, alias);
    try {
      await navigator.clipboard.writeText(url);
    } catch {
      const ta = document.createElement('textarea');
      ta.value = url;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }
  };

  const openShort = (id, alias) => {
    const url = shortUrlFor(id, alias);
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const handleDragStart = (index) => setDragIndex(index);
  const handleDragOver = (e) => e.preventDefault();
  const handleDrop = async (index) => {
    if (dragIndex === null || dragIndex === index) return;
    const reordered = [...links];
    const [moved] = reordered.splice(dragIndex, 1);
    reordered.splice(index, 0, moved);
    setLinks(reordered);
    setDragIndex(null);
    await client.put('/links/reorder', reordered.map((l) => l.id));
    await load();
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Your Links</Typography>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} component="form" onSubmit={create}>
            <TextField label="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required size="small" sx={{ flex: 1 }} />
            <TextField label="URL" value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} required size="small" sx={{ flex: 2 }} placeholder="https://" />
            <TextField label="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} size="small" sx={{ flex: 2 }} />
            <TextField label="Alias (optional)" value={form.alias} onChange={(e) => setForm({ ...form, alias: e.target.value })} size="small" sx={{ flex: 1 }} placeholder="my-alias" />
            <Button type="submit" variant="contained" disabled={!canCreate || loading}>Add</Button>
          </Stack>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mt: 2 }}>
            <TextField
              size="small"
              placeholder="Search title/url/description"
              value={pendingQuery}
              onChange={(e) => setPendingQuery(e.target.value)}
              sx={{ flex: 1 }}
              InputProps={{ startAdornment: (<InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment>) }}
              onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); setQuery(pendingQuery); } }}
            />
            <Button variant="outlined" onClick={() => setQuery(pendingQuery)}>Search</Button>
            {query && <Button variant="text" onClick={() => { setPendingQuery(''); setQuery(''); }}>Clear</Button>}
          </Stack>
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        {links.map((l, idx) => (
          <Grid
            item
            xs={12}
            md={6}
            lg={4}
            key={l.id}
            draggable
            onDragStart={() => handleDragStart(idx)}
            onDragOver={handleDragOver}
            onDrop={() => handleDrop(idx)}
          >
            <Card sx={{ cursor: 'grab' }}>
              <CardContent>
                <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1}>
                      <Box sx={{ flex: 1, pr: 1 }}>
                    {editing === l.id ? (
                      <Stack spacing={1}>
                        <TextField size="small" label="Title" value={editForm.title} onChange={(e) => setEditForm({ ...editForm, title: e.target.value })} />
                        <TextField size="small" label="URL" value={editForm.url} onChange={(e) => setEditForm({ ...editForm, url: e.target.value })} />
                        <TextField size="small" label="Description" value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
                        <TextField size="small" label="Alias (optional)" value={editForm.alias} onChange={(e) => setEditForm({ ...editForm, alias: e.target.value })} placeholder="my-alias" />
                      </Stack>
                    ) : (
                      <>
                        <Stack direction="row" alignItems="center" spacing={1}>
                          <Typography variant="subtitle1">{l.title}</Typography>
                          {typeof l.clickCount === 'number' && (
                            <Typography variant="caption" color="text.secondary">{l.clickCount} clicks</Typography>
                          )}
                        </Stack>
                        <Typography variant="body2" color="text.secondary" noWrap>{l.url}</Typography>
                        {l.description ? (
                          <Typography variant="body2" color="text.secondary" noWrap>{l.description}</Typography>
                        ) : null}
                        {l.alias ? (
                          <Typography variant="body2" color="text.secondary" noWrap>Alias: {l.alias}</Typography>
                        ) : null}
                        <FormControlLabel
                          control={<Switch checked={!!l.isActive} onChange={() => toggleActive(l)} size="small" />}
                          label={l.isActive ? 'Active' : 'Inactive'}
                          sx={{ mt: 1 }}
                        />
                      </>
                    )}
                  </Box>
                  <Stack direction="column" spacing={0.5} alignItems="center">
                    <IconButton size="small" onClick={() => move(idx, -1)} aria-label="move up"><ArrowUpwardIcon fontSize="inherit" /></IconButton>
                    {editing === l.id ? (
                      <IconButton color="primary" onClick={() => saveEdit(l.id)} aria-label="save"><SaveIcon /></IconButton>
                    ) : (
                      <IconButton onClick={() => beginEdit(l)} aria-label="edit"><EditIcon /></IconButton>
                    )}
                    <IconButton color="error" onClick={() => remove(l.id)} aria-label="delete"><DeleteIcon /></IconButton>
                    <Tooltip title="Copy short link">
                      <IconButton onClick={() => copyShort(l.id, l.alias)} aria-label="copy short link"><ContentCopyIcon fontSize="small" /></IconButton>
                    </Tooltip>
                    <Tooltip title="Open short link">
                      <IconButton onClick={() => openShort(l.id, l.alias)} aria-label="open short link"><OpenInNewIcon fontSize="small" /></IconButton>
                    </Tooltip>
                    <IconButton size="small" onClick={() => move(idx, 1)} aria-label="move down"><ArrowDownwardIcon fontSize="inherit" /></IconButton>
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
      {totalPages > 1 && (
        <Box display="flex" justifyContent="center" mt={3}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={(_, p) => { setPage(p); load(p, query); }}
            color="primary"
          />
        </Box>
      )}
    </Box>
  );
};

export default LinkManager;


