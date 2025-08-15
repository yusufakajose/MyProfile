import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Button, Card, CardContent, Grid, IconButton, Stack, TextField, Typography, Switch, FormControlLabel, Tooltip, Pagination, InputAdornment, Divider, Snackbar, Dialog, DialogTitle, DialogContent, DialogActions, MenuItem, Select, InputLabel, FormControl } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import CloseIcon from '@mui/icons-material/Close';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import AddIcon from '@mui/icons-material/Add';
import client from '../api/client';

const Favicon = ({ url, size = 18 }) => {
  const host = useMemo(() => {
    try { return new URL(url).hostname; } catch { return ''; }
  }, [url]);
  const [src, setSrc] = useState(host ? `https://icons.duckduckgo.com/ip3/${host}.ico` : null);
  const [failed, setFailed] = useState(false);
  useEffect(() => { setFailed(false); setSrc(host ? `https://icons.duckduckgo.com/ip3/${host}.ico` : null); }, [host]);
  if (!host || failed || !src) return null;
  return (
    <img
      src={src}
      width={size}
      height={size}
      alt={`favicon of ${host}`}
      loading="lazy"
      style={{ borderRadius: 4 }}
      onError={() => {
        if (src && src.includes('duckduckgo')) setSrc(`https://www.google.com/s2/favicons?domain=${host}&sz=${size * 2}`);
        else setFailed(true);
      }}
    />
  );
};

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
  const [size] = useState(12);
  const [totalPages, setTotalPages] = useState(1);
  const [toast, setToast] = useState({ open: false, message: '' });
  const [qrDialog, setQrDialog] = useState({ open: false, link: null });
  const [qrOptions, setQrOptions] = useState({ format: 'png', size: 256, margin: 1, utm: true, fg: '#000000', bg: '#ffffff', logo: '', ecc: 'M' });
  const [deleteDialog, setDeleteDialog] = useState({ open: false, link: null });
  const createFormRef = useRef(null);
  const CONTRAST_THRESHOLD = 2.5; // match server

  const hexToRgb = (hex) => {
    if (!hex) return { r: 0, g: 0, b: 0 };
    let s = hex.startsWith('#') ? hex.slice(1) : hex;
    if (s.length === 3) s = s.split('').map((c) => c + c).join('');
    const n = parseInt(s, 16);
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
  };
  const srgbToLinear = (c) => (c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
  const relLuminance = ({ r, g, b }) => 0.2126 * srgbToLinear(r / 255) + 0.7152 * srgbToLinear(g / 255) + 0.0722 * srgbToLinear(b / 255);
  const computeContrast = (fgHex, bgHex) => {
    const L1 = relLuminance(hexToRgb(fgHex));
    const L2 = relLuminance(hexToRgb(bgHex));
    const lighter = Math.max(L1, L2);
    const darker = Math.min(L1, L2);
    return (lighter + 0.05) / (darker + 0.05);
  };
  const contrastRatio = computeContrast(qrOptions.fg, qrOptions.bg);
  const contrastOk = contrastRatio >= CONTRAST_THRESHOLD;
  const logoValid = useMemo(() => {
    if (!qrOptions.logo) return true;
    try {
      const u = new URL(qrOptions.logo);
      const p = u.pathname.toLowerCase();
      return u.protocol === 'https:' && (p.endsWith('.png') || p.endsWith('.jpg') || p.endsWith('.jpeg'));
    } catch {
      return false;
    }
  }, [qrOptions.logo]);
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

  const isValidHttpUrl = (s) => {
    try {
      const u = new URL(s);
      return u.protocol === 'http:' || u.protocol === 'https:';
    } catch {
      return false;
    }
  };
  const sanitizeAlias = (alias) => {
    if (!alias) return '';
    let a = alias.trim();
    a = a.replace(/[._-]{2,}/g, '-');
    a = a.replace(/^[._-]+|[._-]+$/g, '');
    return a;
  };
  const isValidAlias = (s) => {
    if (!s) return true;
    const a = sanitizeAlias(s);
    return /^[A-Za-z0-9](?:[A-Za-z0-9-_.]{1,48}[A-Za-z0-9])$/.test(a);
  };
  const canCreate = useMemo(() => {
    return (
      form.title.trim() &&
      isValidHttpUrl((form.url || '').trim()) &&
      isValidAlias((form.alias || '').trim())
    );
  }, [form]);

  const load = useCallback(async (nextPage, nextQuery) => {
    const params = new URLSearchParams({ page: String(nextPage - 1), size: String(size), status, sort });
    if ((nextQuery || '').trim()) params.set('q', (nextQuery || '').trim());
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
  }, [selectedTags, size, sort, status]);

  useEffect(() => { load(1, query); setPage(1); }, [query, selectedTags, status, sort, load]);
  useEffect(() => { (async () => { try { const r = await client.get('/links/tags'); setAllTags(r.data || []); } catch {} })(); }, []);

  const create = async (e) => {
    e.preventDefault();
    if (!canCreate) return;
    setLoading(true);
    try {
      const payload = { ...form };
      // Convert local datetime (yyyy-MM-ddTHH:mm) to ISO if set
      if (payload.startAt) payload.startAt = new Date(payload.startAt).toISOString(); else delete payload.startAt;
      if (payload.endAt) payload.endAt = new Date(payload.endAt).toISOString(); else delete payload.endAt;
      if (!payload.alias || !payload.alias.trim()) {
        delete payload.alias;
      } else {
        const a = sanitizeAlias(payload.alias);
        if (isValidAlias(a)) payload.alias = a; else delete payload.alias;
      }
      await client.post('/links', payload);
      setForm({ title: '', url: '', description: '', alias: '', startAt: '', endAt: '', tags: [] });
      await load(page, query);
      setToast({ open: true, message: 'Link added' });
    } finally {
      setLoading(false);
    }
  };

  const remove = async (id) => {
    await client.delete(`/links/${id}`);
    await load(page, query);
    setToast({ open: true, message: 'Link deleted' });
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
    if (payload.startAt) payload.startAt = new Date(payload.startAt).toISOString(); else payload.startAt = null;
    if (payload.endAt) payload.endAt = new Date(payload.endAt).toISOString(); else payload.endAt = null;
    if (payload.alias && payload.alias.trim()) {
      const a = sanitizeAlias(payload.alias);
      payload.alias = isValidAlias(a) ? a : null;
    }
    await client.put(`/links/${id}`, payload);
    setEditing(null);
    await load();
    setToast({ open: true, message: 'Link saved' });
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
    await load(page, query);
  };

  const shortUrlFor = (id, alias) => alias ? `${redirectOrigin}/r/a/${encodeURIComponent(alias)}` : `${redirectOrigin}/r/${id}`;

  const qrUrlFor = (id, alias, { format = 'png', size = 256, margin = 1, utm = false, fg, bg, logo, ecc } = {}) => {
    const base = alias ? `${redirectOrigin}/r/a/${encodeURIComponent(alias)}/qr.${format}` : `${redirectOrigin}/r/${id}/qr.${format}`;
    const params = new URLSearchParams({ size: String(size), margin: String(margin) });
    if (utm) params.set('utm', '1');
    if (fg) params.set('fg', fg.replace('#', ''));
    if (bg) params.set('bg', bg.replace('#', ''));
    if (logo) params.set('logo', logo);
    if (ecc) params.set('ecc', ecc);
    return `${base}?${params.toString()}`;
  };

  const openQrDialog = (link) => {
    setQrDialog({ open: true, link });
    setQrOptions({ format: 'png', size: 256, margin: 1, utm: true, fg: '#000000', bg: '#ffffff', logo: '', ecc: 'M' });
  };

  const closeQrDialog = () => setQrDialog({ open: false, link: null });

  const downloadQr = async () => {
    const l = qrDialog.link;
    if (!l) return;
    const url = qrUrlFor(l.id, l.alias, qrOptions);
    try {
      const resp = await fetch(url);
      if (!resp.ok) {
        const ct = resp.headers.get('content-type') || '';
        if (ct.includes('application/json')) {
          const j = await resp.json().catch(() => null);
          const msg = j?.message || 'Download failed';
          throw new Error(msg);
        }
        throw new Error('Download failed');
      }
      const blob = await resp.blob();
      const a = document.createElement('a');
      const ext = qrOptions.format === 'svg' ? 'svg' : 'png';
      a.href = URL.createObjectURL(blob);
      a.download = `qr-${l.id}${l.alias ? '-' + l.alias : ''}.${ext}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
    } catch (e) {
      setToast({ open: true, message: e?.message || 'Download failed' });
    }
  };

  const copyQrImage = async () => {
    const l = qrDialog.link;
    if (!l) return;
    const url = qrUrlFor(l.id, l.alias, qrOptions);
    try {
      const resp = await fetch(url);
      if (!resp.ok) {
        const ct = resp.headers.get('content-type') || '';
        if (ct.includes('application/json')) {
          const j = await resp.json().catch(() => null);
          const msg = j?.message || 'Failed to copy image';
          throw new Error(msg);
        }
        throw new Error('Failed to copy image');
      }
      const blob = await resp.blob();
      if (navigator.clipboard && 'write' in navigator.clipboard) {
        const item = new ClipboardItem({ [blob.type]: blob });
        await navigator.clipboard.write([item]);
        setToast({ open: true, message: 'QR copied to clipboard' });
      } else {
        // Fallback: open in new tab
        const objectUrl = URL.createObjectURL(blob);
        window.open(objectUrl, '_blank');
      }
    } catch (e) {
      setToast({ open: true, message: e?.message || 'Copy failed' });
    }
  };

  const copyShort = async (id, alias) => {
    const url = shortUrlFor(id, alias);
    try {
      await navigator.clipboard.writeText(url);
      setToast({ open: true, message: 'Short link copied' });
    } catch {
      const ta = document.createElement('textarea');
      ta.value = url;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      setToast({ open: true, message: 'Short link copied' });
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
      <Box sx={{ position: 'fixed', right: 16, bottom: 16, display: { xs: 'block', sm: 'none' }, zIndex: 1200 }}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => createFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })} sx={{ borderRadius: 999 }}>New link</Button>
      </Box>
      <Card sx={{ mb: 3 }} ref={createFormRef}>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} component="form" onSubmit={create}>
            <TextField label="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required size="small" sx={{ flex: 1 }} />
            <TextField label="URL" value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} required size="small" sx={{ flex: 2 }} placeholder="https://" error={!!form.url && !isValidHttpUrl(form.url)} helperText={!!form.url && !isValidHttpUrl(form.url) ? 'Enter a valid http(s) URL' : ' '} />
            <TextField label="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} size="small" sx={{ flex: 2 }} />
            <TextField label="Alias (optional)" value={form.alias} onChange={(e) => setForm({ ...form, alias: e.target.value })} size="small" sx={{ flex: 1 }} placeholder="my-alias"
              error={!isValidAlias(form.alias || '')}
              helperText={!isValidAlias(form.alias || '') ? 'Alias: 3–50 chars, start/end with letter/number; -, _, . allowed inside' : ' '}
              onBlur={() => { if (!isValidAlias(form.alias || '')) setToast({ open: true, message: 'Invalid alias: 3–50 chars, start/end alphanumeric; -, _, . inside only' }); }}
            />
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
        {links.length === 0 && (
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Stack spacing={1} alignItems="flex-start">
                  <Typography variant="subtitle1">No links yet</Typography>
                  <Typography variant="body2" color="text.secondary">Create your first link to get started.</Typography>
                  <Button variant="contained" onClick={() => createFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })}>Create link</Button>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        )}
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
                        <TextField size="small" label="URL" value={editForm.url} onChange={(e) => setEditForm({ ...editForm, url: e.target.value })} error={!!editForm.url && !isValidHttpUrl(editForm.url)} helperText={!!editForm.url && !isValidHttpUrl(editForm.url) ? 'Enter a valid http(s) URL' : ' '} />
                        <TextField size="small" label="Description" value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
                        <TextField size="small" label="Alias (optional)" value={editForm.alias} onChange={(e) => setEditForm({ ...editForm, alias: e.target.value })} placeholder="my-alias"
                          error={!isValidAlias(editForm.alias || '')}
                          helperText={!isValidAlias(editForm.alias || '') ? 'Alias: 3–50 chars, start/end with letter/number; -, _, . allowed inside' : ' '}
                          onBlur={() => { if (!isValidAlias(editForm.alias || '')) setToast({ open: true, message: 'Invalid alias: 3–50 chars, start/end alphanumeric; -, _, . inside only' }); }}
                        />
                        <TextField size="small" label="Tags (comma separated)" value={(editForm.tags || []).join(', ')} onChange={(e) => setEditForm({ ...editForm, tags: e.target.value.split(',').map(s => s.trim()).filter(Boolean) })} placeholder="news, personal" />
                        <TextField type="datetime-local" size="small" label="Start at" value={editForm.startAt} onChange={(e) => setEditForm({ ...editForm, startAt: e.target.value })} InputLabelProps={{ shrink: true }} />
                        <TextField type="datetime-local" size="small" label="End at" value={editForm.endAt} onChange={(e) => setEditForm({ ...editForm, endAt: e.target.value })} InputLabelProps={{ shrink: true }} />
                      </Stack>
                    ) : (
                      <>
                        <Stack direction="row" alignItems="center" spacing={1}>
                          <Favicon url={l.url} size={18} />
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
                    <IconButton size="small" onClick={() => move(idx, -1)} aria-label="move up" disabled={idx === 0}><ArrowUpwardIcon fontSize="inherit" /></IconButton>
                    {editing === l.id ? (
                      <>
                        <IconButton color="primary" onClick={() => saveEdit(l.id)} aria-label="save" disabled={!(editForm.title || '').trim() || !isValidHttpUrl(editForm.url || '') || !isValidAlias(editForm.alias || '')}><SaveIcon /></IconButton>
                        <IconButton onClick={() => { setEditing(null); }} aria-label="cancel"><CloseIcon /></IconButton>
                      </>
                    ) : (
                      <IconButton onClick={() => beginEdit(l)} aria-label="edit"><EditIcon /></IconButton>
                    )}
                    <IconButton color="error" onClick={() => setDeleteDialog({ open: true, link: l })} aria-label="delete"><DeleteIcon /></IconButton>
                    <Tooltip title="Copy short link">
                      <IconButton onClick={() => copyShort(l.id, l.alias)} aria-label="copy short link"><ContentCopyIcon fontSize="small" /></IconButton>
                    </Tooltip>
                    <Tooltip title="Open short link">
                      <IconButton onClick={() => openShort(l.id, l.alias)} aria-label="open short link"><OpenInNewIcon fontSize="small" /></IconButton>
                    </Tooltip>
                    <Tooltip title="Get QR code">
                      <IconButton onClick={() => openQrDialog(l)} aria-label="get qr"><QrCode2Icon fontSize="small" /></IconButton>
                    </Tooltip>
                    <IconButton size="small" onClick={() => move(idx, 1)} aria-label="move down" disabled={idx === links.length - 1}><ArrowDownwardIcon fontSize="inherit" /></IconButton>
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
                  <Box sx={{ width: '100%', overflowX: 'auto' }}>
                    <Box component="table" sx={{ width: '100%', minWidth: 720, borderCollapse: 'collapse' }}>
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
      <Snackbar
        open={toast.open}
        autoHideDuration={2000}
        onClose={() => setToast({ open: false, message: '' })}
        message={toast.message}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />

      <Dialog open={qrDialog.open} onClose={closeQrDialog} fullWidth maxWidth="xs">
        <DialogTitle>Download QR code</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl size="small">
              <InputLabel id="qr-format-label">Format</InputLabel>
              <Select labelId="qr-format-label" label="Format" value={qrOptions.format} onChange={(e) => setQrOptions((o) => ({ ...o, format: e.target.value }))}>
                <MenuItem value="png">PNG</MenuItem>
                <MenuItem value="svg">SVG</MenuItem>
              </Select>
            </FormControl>
            <FormControl size="small">
              <InputLabel id="qr-ecc-label">Error correction</InputLabel>
              <Select labelId="qr-ecc-label" label="Error correction" value={qrOptions.ecc} onChange={(e) => setQrOptions((o) => ({ ...o, ecc: e.target.value }))}>
                <MenuItem value="L">L (low)</MenuItem>
                <MenuItem value="M">M (medium)</MenuItem>
                <MenuItem value="Q">Q (quartile)</MenuItem>
                <MenuItem value="H">H (high)</MenuItem>
              </Select>
            </FormControl>
            <TextField size="small" type="number" label="Size (px)" value={qrOptions.size} onChange={(e) => setQrOptions((o) => ({ ...o, size: Math.max(128, Math.min(1024, parseInt(e.target.value || '0', 10))) }))} />
            <TextField size="small" type="number" label="Margin" value={qrOptions.margin} onChange={(e) => setQrOptions((o) => ({ ...o, margin: Math.max(0, Math.min(4, parseInt(e.target.value || '0', 10))) }))} />
            <FormControlLabel control={<Switch size="small" checked={!!qrOptions.utm} onChange={(e) => setQrOptions((o) => ({ ...o, utm: e.target.checked }))} />} label="Append UTM (qr/linkgrove)" />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField size="small" type="color" label="FG" value={qrOptions.fg} onChange={(e) => setQrOptions((o) => ({ ...o, fg: e.target.value }))} sx={{ width: 160 }} InputLabelProps={{ shrink: true }} />
              <TextField size="small" type="color" label="BG" value={qrOptions.bg} onChange={(e) => setQrOptions((o) => ({ ...o, bg: e.target.value }))} sx={{ width: 160 }} InputLabelProps={{ shrink: true }} />
            </Stack>
            <TextField size="small" label="Logo URL (PNG/JPG)" value={qrOptions.logo} onChange={(e) => setQrOptions((o) => ({ ...o, logo: e.target.value }))} placeholder="https://.../logo.png" error={!logoValid} helperText={!logoValid ? 'Use https and .png/.jpg/.jpeg' : ' '} />
            <Box>
              <Typography variant="caption" color={contrastOk ? 'success.main' : 'error.main'}>
                Contrast: {contrastRatio.toFixed(2)} {contrastOk ? '(ok)' : `(too low, need ≥ ${CONTRAST_THRESHOLD})`}
              </Typography>
            </Box>
            {qrDialog.link && (
              <Box>
                <Typography variant="caption" color="text.secondary">Preview URL:</Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{qrUrlFor(qrDialog.link.id, qrDialog.link.alias, qrOptions)}</Typography>
                <Box sx={{ mt: 1, display: 'flex', justifyContent: 'center' }}>
                  {qrOptions.format === 'svg' ? (
                    <img alt="QR preview" src={qrUrlFor(qrDialog.link.id, qrDialog.link.alias, qrOptions)} style={{ maxWidth: '100%', height: 'auto' }} />
                  ) : (
                    <img alt="QR preview" src={qrUrlFor(qrDialog.link.id, qrDialog.link.alias, qrOptions)} width={Math.min(300, qrOptions.size)} height={Math.min(300, qrOptions.size)} style={{ imageRendering: 'pixelated' }} />
                  )}
                </Box>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={copyQrImage} disabled={!contrastOk || !logoValid}>Copy image</Button>
          <Button onClick={closeQrDialog}>Close</Button>
          <Button variant="contained" onClick={downloadQr} disabled={!contrastOk || !logoValid}>Download</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={deleteDialog.open} onClose={() => setDeleteDialog({ open: false, link: null })} maxWidth="xs" fullWidth>
        <DialogTitle>Delete link?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">This action cannot be undone.</Typography>
          {deleteDialog.link && (
            <Typography variant="subtitle2" sx={{ mt: 1 }}>{deleteDialog.link.title}</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialog({ open: false, link: null })}>Cancel</Button>
          <Button color="error" variant="contained" onClick={async () => { const id = deleteDialog.link?.id; setDeleteDialog({ open: false, link: null }); if (id != null) await remove(id); }}>Delete</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default LinkManager;


