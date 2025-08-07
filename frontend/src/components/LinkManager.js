import React, { useEffect, useMemo, useState } from 'react';
import { Box, Button, Card, CardContent, Grid, IconButton, Stack, TextField, Typography } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import client from '../api/client';

const LinkManager = () => {
  const [links, setLinks] = useState([]);
  const [form, setForm] = useState({ title: '', url: '', description: '' });
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null); // id being edited
  const [editForm, setEditForm] = useState({ title: '', url: '', description: '' });

  const canCreate = useMemo(() => {
    return form.title.trim() && /^https?:\/\//i.test(form.url.trim());
  }, [form]);

  const load = async () => {
    const res = await client.get('/links');
    setLinks(res.data || []);
  };

  useEffect(() => { load(); }, []);

  const create = async (e) => {
    e.preventDefault();
    if (!canCreate) return;
    setLoading(true);
    try {
      await client.post('/links', form);
      setForm({ title: '', url: '', description: '' });
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
    setEditForm({ title: link.title || '', url: link.url || '', description: link.description || '' });
  };

  const saveEdit = async (id) => {
    await client.put(`/links/${id}`, editForm);
    setEditing(null);
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

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Your Links</Typography>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} component="form" onSubmit={create}>
            <TextField label="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required size="small" sx={{ flex: 1 }} />
            <TextField label="URL" value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} required size="small" sx={{ flex: 2 }} placeholder="https://" />
            <TextField label="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} size="small" sx={{ flex: 2 }} />
            <Button type="submit" variant="contained" disabled={!canCreate || loading}>Add</Button>
          </Stack>
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        {links.map((l, idx) => (
          <Grid item xs={12} md={6} lg={4} key={l.id}>
            <Card>
              <CardContent>
                <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1}>
                  <Box sx={{ flex: 1, pr: 1 }}>
                    {editing === l.id ? (
                      <Stack spacing={1}>
                        <TextField size="small" label="Title" value={editForm.title} onChange={(e) => setEditForm({ ...editForm, title: e.target.value })} />
                        <TextField size="small" label="URL" value={editForm.url} onChange={(e) => setEditForm({ ...editForm, url: e.target.value })} />
                        <TextField size="small" label="Description" value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
                      </Stack>
                    ) : (
                      <>
                        <Typography variant="subtitle1">{l.title}</Typography>
                        <Typography variant="body2" color="text.secondary" noWrap>{l.url}</Typography>
                        {l.description ? (
                          <Typography variant="body2" color="text.secondary" noWrap>{l.description}</Typography>
                        ) : null}
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
                    <IconButton size="small" onClick={() => move(idx, 1)} aria-label="move down"><ArrowDownwardIcon fontSize="inherit" /></IconButton>
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default LinkManager;


