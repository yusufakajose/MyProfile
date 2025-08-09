import React, { useEffect, useMemo, useState } from 'react';
import { Box, Button, Card, CardContent, Grid, IconButton, Stack, TextField, Typography, Switch, FormControlLabel, Tooltip, Pagination, InputAdornment, Divider } from '@mui/material';
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
  const [form, setForm] = useState({ title: '', url: '', description: '', alias: '', startAt: '', endAt: '', tags: [] });
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null); // id being edited
  const [editForm, setEditForm] = useState({ title: '', url: '', description: '', alias: '', startAt: '', endAt: '', tags: [] });
  const [allTags, setAllTags] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [status, setStatus] = useState('all');
  const [sort, setSort] = useState('order');
  const [dragIndex, setDragIndex] = useState(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(12);
  const [totalPages, setTotalPages] = useState(1);
  const [query, setQuery] = useState('');
  const [pendingQuery, setPendingQuery] = useState('');
  // Variants UI state
  const [variantsOpenFor, setVariantsOpenFor] = useState(null); // linkId or null
  const [variantsByLink, setVariantsByLink] = useState({}); // { [linkId]: Variant[] }
  const [newVariantFormByLink, setNewVariantFormByLink] = useState({}); // { [linkId]: {title,url,description,weight,isActive} }
  const [variantEdits, setVariantEdits] = useState({}); // { [variantId]: {title,url,description,weight,isActive} }

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
    const params = new URLSearchParams({ page: String(nextPage - 1), size: String(size), status, sort });
    if (nextQuery.trim()) params.set('q', nextQuery.trim());
    if (selectedTags.length) selectedTags.forEach(t => params.append('tags', t));
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

  useEffect(() => { load(1, query); setPage(1); }, [query, size, selectedTags, status, sort]);
  useEffect(() => { (async () => { try { const r = await client.get('/links/tags'); setAllTags(r.data || []); } catch {} })(); }, []);

  const create = async (e) => {
    e.preventDefault();
    if (!canCreate) return;
    setLoading(true);
    try {
      const payload = { ...form };
      // Convert local datetime (yyyy-MM-ddTHH:mm) to ISO if set
      if (payload.startAt) payload.startAt = new Date(payload.startAt).toISOString();
      if (payload.endAt) payload.endAt = new Date(payload.endAt).toISOString();
      await client.post('/links', payload);
      setForm({ title: '', url: '', description: '', alias: '', startAt: '', endAt: '', tags: [] });
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
    setEditForm({
      title: link.title || '',
      url: link.url || '',
      description: link.description || '',
      alias: link.alias || '',
      startAt: link.startAt ? link.startAt.slice(0,16) : '',
      endAt: link.endAt ? link.endAt.slice(0,16) : '',
    });
  };

  const saveEdit = async (id) => {
    const payload = { ...editForm };
    if (payload.startAt) payload.startAt = new Date(payload.startAt).toISOString();
    if (payload.endAt) payload.endAt = new Date(payload.endAt).toISOString();
    await client.put(`/links/${id}`, payload);
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

  // Variants helpers
  const loadVariants = async (linkId) => {
    try {
      const res = await client.get(`/links/${linkId}/variants`);
      setVariantsByLink((prev) => ({ ...prev, [linkId]: res.data || [] }));
    } catch (e) {
      setVariantsByLink((prev) => ({ ...prev, [linkId]: [] }));
    }
  };

  const toggleVariants = async (linkId) => {
    const next = variantsOpenFor === linkId ? null : linkId;
    setVariantsOpenFor(next);
    if (next) {
      await loadVariants(next);
      setNewVariantFormByLink((prev) => ({ ...prev, [next]: prev[next] || { title: '', url: '', description: '', weight: 1, isActive: true } }));
    }
  };

  const setNewVariantField = (linkId, field, value) => {
    setNewVariantFormByLink((prev) => ({
      ...prev,
      [linkId]: { ...(prev[linkId] || { title: '', url: '', weight: 1, isActive: true }), [field]: value }
    }));
  };

  const addVariant = async (linkId) => {
    const f = newVariantFormByLink[linkId] || { title: '', url: '', description: '', weight: 1, isActive: true };
    if (!f.title?.trim() || !/^https?:\/\//i.test(f.url || '')) return;
    await client.post(`/links/${linkId}/variants`, {
      title: f.title.trim(),
      url: f.url.trim(),
      description: f.description?.trim() || '',
      weight: Number.isFinite(+f.weight) ? Math.max(0, parseInt(f.weight, 10)) : 1,
      isActive: !!f.isActive,
    });
    await loadVariants(linkId);
    setNewVariantFormByLink((prev) => ({ ...prev, [linkId]: { title: '', url: '', description: '', weight: 1, isActive: true } }));
  };

  const setVariantEditField = (variantId, field, value) => {
    setVariantEdits((prev) => ({
      ...prev,
      [variantId]: { ...(prev[variantId] || {}), [field]: value }
    }));
  };

  const saveVariant = async (linkId, variant) => {
    const draft = variantEdits[variant.id] || {};
    const payload = {
      title: draft.title ?? variant.title,
      url: draft.url ?? variant.url,
      description: draft.description ?? variant.description,
      weight: draft.weight != null ? Math.max(0, parseInt(draft.weight, 10)) : variant.weight,
      isActive: draft.isActive != null ? !!draft.isActive : variant.isActive,
    };
    await client.put(`/links/${linkId}/variants/${variant.id}`, payload);
    await loadVariants(linkId);
  };

  const deleteVariant = async (linkId, variantId) => {
    await client.delete(`/links/${linkId}/variants/${variantId}`);
    await loadVariants(linkId);
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
            <TextField label="Tags (comma separated)" value={(form.tags || []).join(', ')} onChange={(e) => setForm({ ...form, tags: e.target.value.split(',').map(s => s.trim()).filter(Boolean) })} size="small" sx={{ flex: 2 }} placeholder="news, personal" />
            <TextField type="datetime-local" label="Start at" value={form.startAt} onChange={(e) => setForm({ ...form, startAt: e.target.value })} size="small" sx={{ flex: 1 }} InputLabelProps={{ shrink: true }} />
            <TextField type="datetime-local" label="End at" value={form.endAt} onChange={(e) => setForm({ ...form, endAt: e.target.value })} size="small" sx={{ flex: 1 }} InputLabelProps={{ shrink: true }} />
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
            <TextField select size="small" label="Status" value={status} onChange={(e) => setStatus(e.target.value)} sx={{ width: 150 }} SelectProps={{ native: true }}>
              <option value="all">All</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </TextField>
            <TextField select size="small" label="Sort" value={sort} onChange={(e) => setSort(e.target.value)} sx={{ width: 180 }} SelectProps={{ native: true }}>
              <option value="order">Order</option>
              <option value="created_desc">Created ↓</option>
              <option value="updated_desc">Updated ↓</option>
              <option value="clicks_desc">Clicks ↓</option>
            </TextField>
            <Button variant="text" onClick={async () => {
              const params = new URLSearchParams({ sort, status });
              if (query.trim()) params.set('q', query.trim());
              selectedTags.forEach(t => params.append('tags', t));
              const url = `${client.defaults.baseURL}/links/export?${params.toString()}`;
              const tokenStr = localStorage.getItem('auth');
              const token = tokenStr ? JSON.parse(tokenStr).token : null;
              const resp = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
              const text = await resp.text();
              const blob = new Blob([text], { type: 'text/csv;charset=utf-8' });
              const a = document.createElement('a');
              a.href = URL.createObjectURL(blob);
              a.download = 'links.csv';
              document.body.appendChild(a);
              a.click();
              a.remove();
            }}>Export CSV</Button>
          </Stack>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mt: 1, flexWrap: 'wrap' }}>
            {allTags.map((t) => (
              <Button key={t} size="small" variant={selectedTags.includes(t) ? 'contained' : 'outlined'} onClick={() => setSelectedTags((prev) => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t])}>{t}</Button>
            ))}
            {allTags.length === 0 && <Typography variant="caption" color="text.secondary">No tags yet</Typography>}
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
                        <TextField size="small" label="Tags (comma separated)" value={(editForm.tags || []).join(', ')} onChange={(e) => setEditForm({ ...editForm, tags: e.target.value.split(',').map(s => s.trim()).filter(Boolean) })} placeholder="news, personal" />
                        <TextField type="datetime-local" size="small" label="Start at" value={editForm.startAt} onChange={(e) => setEditForm({ ...editForm, startAt: e.target.value })} InputLabelProps={{ shrink: true }} />
                        <TextField type="datetime-local" size="small" label="End at" value={editForm.endAt} onChange={(e) => setEditForm({ ...editForm, endAt: e.target.value })} InputLabelProps={{ shrink: true }} />
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
                        {(l.tags && l.tags.length > 0) ? (
                          <Typography variant="body2" color="text.secondary" noWrap>Tags: {l.tags.join(', ')}</Typography>
                        ) : null}
                        {(l.startAt || l.endAt) ? (
                          <Typography variant="body2" color="text.secondary" noWrap>
                            {l.startAt ? `From ${new Date(l.startAt).toLocaleString()}` : 'From any time'}
                            {l.endAt ? ` to ${new Date(l.endAt).toLocaleString()}` : ''}
                          </Typography>
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
              {variantsOpenFor === l.id && (
                <CardContent sx={{ pt: 0 }}>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Variants</Typography>
                  {/* New variant form */}
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mb: 1 }}>
                    <TextField size="small" label="Title" value={(newVariantFormByLink[l.id]?.title) || ''} onChange={(e) => setNewVariantField(l.id, 'title', e.target.value)} sx={{ flex: 1 }} />
                    <TextField size="small" label="URL" value={(newVariantFormByLink[l.id]?.url) || ''} onChange={(e) => setNewVariantField(l.id, 'url', e.target.value)} sx={{ flex: 2 }} placeholder="https://" />
                    <TextField size="small" label="Description" value={(newVariantFormByLink[l.id]?.description) || ''} onChange={(e) => setNewVariantField(l.id, 'description', e.target.value)} sx={{ flex: 2 }} />
                    <TextField size="small" label="Weight" type="number" inputProps={{ min: 0 }} value={(newVariantFormByLink[l.id]?.weight) ?? 1} onChange={(e) => setNewVariantField(l.id, 'weight', e.target.value)} sx={{ width: 120 }} />
                    <FormControlLabel control={<Switch size="small" checked={!!(newVariantFormByLink[l.id]?.isActive)} onChange={(e) => setNewVariantField(l.id, 'isActive', e.target.checked)} />} label={newVariantFormByLink[l.id]?.isActive ? 'Active' : 'Inactive'} />
                    <Button variant="contained" size="small" onClick={() => addVariant(l.id)}>Add Variant</Button>
                  </Stack>
                  {/* Variants list */}
                  <Box component="table" sx={{ width: '100%', borderCollapse: 'collapse' }}>
                    <Box component="thead">
                      <Box component="tr">
                        <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Title</Box>
                        <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>URL</Box>
                        <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Description</Box>
                        <Box component="th" sx={{ textAlign: 'right', pb: 1, width: 120 }}>Weight</Box>
                        <Box component="th" sx={{ textAlign: 'center', pb: 1, width: 120 }}>Active</Box>
                        <Box component="th" sx={{ textAlign: 'right', pb: 1, width: 160 }}>Actions</Box>
                      </Box>
                    </Box>
                    <Box component="tbody">
                      {(variantsByLink[l.id] || []).map((v) => (
                        <Box component="tr" key={v.id}>
                          <Box component="td" sx={{ py: 0.5 }}>
                            <TextField size="small" value={(variantEdits[v.id]?.title) ?? v.title} onChange={(e) => setVariantEditField(v.id, 'title', e.target.value)} />
                          </Box>
                          <Box component="td" sx={{ py: 0.5 }}>
                            <TextField size="small" value={(variantEdits[v.id]?.url) ?? v.url} onChange={(e) => setVariantEditField(v.id, 'url', e.target.value)} sx={{ minWidth: 220 }} />
                          </Box>
                          <Box component="td" sx={{ py: 0.5 }}>
                            <TextField size="small" value={(variantEdits[v.id]?.description) ?? (v.description || '')} onChange={(e) => setVariantEditField(v.id, 'description', e.target.value)} sx={{ minWidth: 200 }} />
                          </Box>
                          <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>
                            <TextField size="small" type="number" inputProps={{ min: 0 }} value={(variantEdits[v.id]?.weight) ?? v.weight} onChange={(e) => setVariantEditField(v.id, 'weight', e.target.value)} sx={{ width: 100 }} />
                          </Box>
                          <Box component="td" sx={{ py: 0.5, textAlign: 'center' }}>
                            <Switch size="small" checked={(variantEdits[v.id]?.isActive) ?? v.isActive} onChange={(e) => setVariantEditField(v.id, 'isActive', e.target.checked)} />
                          </Box>
                          <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>
                            <Stack direction="row" spacing={1} justifyContent="flex-end">
                              <Button size="small" variant="outlined" onClick={() => saveVariant(l.id, v)}>Save</Button>
                              <Button size="small" color="error" variant="text" onClick={() => deleteVariant(l.id, v.id)}>Delete</Button>
                            </Stack>
                          </Box>
                        </Box>
                      ))}
                      {(variantsByLink[l.id] || []).length === 0 && (
                        <Box component="tr"><Box component="td" colSpan={6} sx={{ py: 1, color: 'text.secondary' }}>No variants yet</Box></Box>
                      )}
                    </Box>
                  </Box>
                </CardContent>
              )}
              <CardContent sx={{ pt: 0 }}>
                <Button size="small" variant="text" onClick={() => toggleVariants(l.id)}>
                  {variantsOpenFor === l.id ? 'Hide Variants' : 'Manage Variants'}
                </Button>
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


