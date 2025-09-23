import React, { useEffect, useState } from 'react';
import { Box, Card, CardContent, TextField, Button, Typography, Alert, Stack, Avatar, ToggleButtonGroup, ToggleButton } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';

const ProfileSettings = () => {
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [profileImageUrl, setProfileImageUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [username, setUsername] = useState('');
  const [emailVerified, setEmailVerified] = useState(false);
  const { login, user: authUser } = useAuth();
  const [themePrimaryColor, setThemePrimaryColor] = useState('');
  const [themeAccentColor, setThemeAccentColor] = useState('');
  const [themeBackgroundColor, setThemeBackgroundColor] = useState('');
  const [themeTextColor, setThemeTextColor] = useState('');
  const [themePreset, setThemePreset] = useState('custom');

  const presets = [
    { id: 'custom', label: 'Custom', primary: '', accent: '', background: '', text: '' },
    { id: 'light', label: 'Light', primary: '#1976d2', accent: '#1f2937', background: '#ffffff', text: '#111827' },
    { id: 'dark', label: 'Dark', primary: '#90caf9', accent: '#f28b82', background: '#0f172a', text: '#e2e8f0' },
    { id: 'ocean', label: 'Ocean', primary: '#0ea5e9', accent: '#22d3ee', background: '#082f49', text: '#f1f5f9' },
    { id: 'sunset', label: 'Sunset', primary: '#f97316', accent: '#facc15', background: '#1f2937', text: '#f8fafc' },
    { id: 'forest', label: 'Forest', primary: '#16a34a', accent: '#a3e635', background: '#0b3d2e', text: '#f1f5f9' }
  ];

  const applyPreset = (presetId) => {
    const preset = presets.find((p) => p.id === presetId) || presets[0];
    setThemePreset(preset.id);
    setThemePrimaryColor(preset.primary);
    setThemeAccentColor(preset.accent);
    setThemeBackgroundColor(preset.background);
    setThemeTextColor(preset.text);
  };

  const handleColorChange = (setter) => (event) => {
    const value = event.target.value;
    setter(value);
    setThemePreset('custom');
  };

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await client.get('/user/profile');
        setDisplayName(res.data.displayName || '');
        setBio(res.data.bio || '');
        setProfileImageUrl(res.data.profileImageUrl || '');
        setUsername(res.data.username || authUser?.username || '');
        setEmailVerified(Boolean(res.data.emailVerified));
        const primary = res.data.themePrimaryColor || '';
        const accent = res.data.themeAccentColor || '';
        const background = res.data.themeBackgroundColor || '';
        const text = res.data.themeTextColor || '';
        setThemePrimaryColor(primary);
        setThemeAccentColor(accent);
        setThemeBackgroundColor(background);
        setThemeTextColor(text);
        const matchedPreset = presets.find((p) => p.primary === primary && p.accent === accent && p.background === background && p.text === text);
        setThemePreset(matchedPreset ? matchedPreset.id : 'custom');
      } catch (e) {
        setError('Failed to load profile');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [authUser?.username]);

  const save = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const res = await client.put('/user/profile', { displayName, bio, profileImageUrl, themePrimaryColor, themeAccentColor, themeBackgroundColor, themeTextColor });
      setSuccess('Saved');
      setDisplayName(res.data.displayName || '');
      setBio(res.data.bio || '');
      setProfileImageUrl(res.data.profileImageUrl || '');
      const primary = res.data.themePrimaryColor || '';
      const accent = res.data.themeAccentColor || '';
      const background = res.data.themeBackgroundColor || '';
      const text = res.data.themeTextColor || '';
      setThemePrimaryColor(primary);
      setThemeAccentColor(accent);
      setThemeBackgroundColor(background);
      setThemeTextColor(text);
      const matchedPreset = presets.find((p) => p.primary === primary && p.accent === accent && p.background === background && p.text === text);
      setThemePreset(matchedPreset ? matchedPreset.id : 'custom');
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to save');
    } finally {
      setLoading(false);
    }
  };

  const saveUsername = async () => {
    if (!username || username === authUser?.username) return;
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const res = await client.put('/user/username', { newUsername: username });
      // Refresh auth token and user in context
      login(res.data.token, { ...(authUser || {}), username: res.data.username });
      setSuccess('Username updated');
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to update username');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box maxWidth={720} mx="auto">
      <Typography variant="h5" gutterBottom>Profile Settings</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
      <Card>
        <CardContent>
          <Stack component="form" spacing={2} onSubmit={save}>
            <Alert severity={emailVerified ? 'success' : 'warning'}>
              {emailVerified ? 'Email verified' : 'Email not verified yet. Check your inbox or request a new verification email.'}
            </Alert>
            <TextField
              label="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              helperText="Public profile url: /u/username"
            />
            {!emailVerified && (
              <Box>
                <Button variant="outlined" onClick={async () => {
                  setError(''); setSuccess('');
                  try {
                    await client.post('/auth/initiate-email-verification', null, { params: { username } });
                    setSuccess('Verification email sent');
                  } catch (e) {
                    setError('Failed to send verification email');
                  }
                }}>Send Verification Email</Button>
              </Box>
            )}
            <Box>
              <Button variant="outlined" onClick={saveUsername} disabled={loading || !username || username === authUser?.username}>Update Username</Button>
            </Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'flex-start', sm: 'center' }}>
              <Avatar src={profileImageUrl || undefined} sx={{ width: 64, height: 64 }} />
              <TextField
                label="Avatar URL"
                value={profileImageUrl}
                onChange={(e) => setProfileImageUrl(e.target.value)}
                fullWidth
              />
            </Stack>
            <TextField
              label="Display Name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              fullWidth
            />
            <TextField
              label="Bio"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              fullWidth
              multiline
              minRows={3}
            />
            <Typography variant="subtitle1">Theme</Typography>
            <ToggleButtonGroup
              exclusive
              value={themePreset}
              onChange={(event, value) => {
                if (value) {
                  applyPreset(value);
                }
              }}
              aria-label="Theme presets"
              sx={{ flexWrap: 'wrap', gap: 1 }}
            >
              {presets.map((preset) => (
                <ToggleButton key={preset.id} value={preset.id} aria-label={preset.label} sx={{ textTransform: 'none', flexGrow: 1, minWidth: 120 }}>
                  {preset.label}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField label="Primary Color" value={themePrimaryColor} onChange={handleColorChange(setThemePrimaryColor)} placeholder="#1976d2" fullWidth />
              <TextField label="Accent Color" value={themeAccentColor} onChange={handleColorChange(setThemeAccentColor)} placeholder="#ff4081" fullWidth />
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField label="Background Color" value={themeBackgroundColor} onChange={handleColorChange(setThemeBackgroundColor)} placeholder="#ffffff" fullWidth />
              <TextField label="Text Color" value={themeTextColor} onChange={handleColorChange(setThemeTextColor)} placeholder="#111827" fullWidth />
            </Stack>
            <Box sx={{ mt: 1 }}>
              <Typography variant="caption" color="text.secondary">Live preview</Typography>
              <Box sx={{
                mt: 1,
                p: 2,
                borderRadius: 2,
                backgroundColor: themeBackgroundColor || '#ffffff',
                color: themeTextColor || '#111827',
                border: '1px solid #e5e7eb'
              }}>
                <Typography variant="subtitle1" sx={{ color: themePrimaryColor || '#1976d2' }}>Primary heading</Typography>
                <Typography variant="body2">Sample body text</Typography>
                <Button size="small" sx={{ mt: 1, backgroundColor: themePrimaryColor || '#1976d2', color: '#fff', ':hover': { backgroundColor: themePrimaryColor || '#1976d2' } }}>Primary Button</Button>
                <Button size="small" sx={{ mt: 1, ml: 1, color: themeAccentColor || '#ff4081' }}>Accent Link</Button>
              </Box>
            </Box>
            <Box>
              <Button type="submit" variant="contained" disabled={loading}>Save</Button>
            </Box>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ProfileSettings;


