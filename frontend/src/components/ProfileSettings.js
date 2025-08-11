import React, { useEffect, useState } from 'react';
import { Box, Card, CardContent, TextField, Button, Typography, Alert, Stack, Avatar } from '@mui/material';
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
  const { login, user: authUser } = useAuth();
  const [themePrimaryColor, setThemePrimaryColor] = useState('');
  const [themeAccentColor, setThemeAccentColor] = useState('');
  const [themeBackgroundColor, setThemeBackgroundColor] = useState('');
  const [themeTextColor, setThemeTextColor] = useState('');

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
        setThemePrimaryColor(res.data.themePrimaryColor || '');
        setThemeAccentColor(res.data.themeAccentColor || '');
        setThemeBackgroundColor(res.data.themeBackgroundColor || '');
        setThemeTextColor(res.data.themeTextColor || '');
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
      setThemePrimaryColor(res.data.themePrimaryColor || '');
      setThemeAccentColor(res.data.themeAccentColor || '');
      setThemeBackgroundColor(res.data.themeBackgroundColor || '');
      setThemeTextColor(res.data.themeTextColor || '');
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
            <TextField
              label="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              helperText="Public profile url: /u/username"
            />
            <Box>
              <Button variant="outlined" onClick={saveUsername} disabled={loading || !username || username === authUser?.username}>Update Username</Button>
            </Box>
            <Box display="flex" alignItems="center" gap={2}>
              <Avatar src={profileImageUrl || undefined} sx={{ width: 64, height: 64 }} />
              <TextField
                label="Avatar URL"
                value={profileImageUrl}
                onChange={(e) => setProfileImageUrl(e.target.value)}
                fullWidth
              />
            </Box>
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
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField label="Primary Color" value={themePrimaryColor} onChange={(e) => setThemePrimaryColor(e.target.value)} placeholder="#1976d2" fullWidth />
              <TextField label="Accent Color" value={themeAccentColor} onChange={(e) => setThemeAccentColor(e.target.value)} placeholder="#ff4081" fullWidth />
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField label="Background Color" value={themeBackgroundColor} onChange={(e) => setThemeBackgroundColor(e.target.value)} placeholder="#ffffff" fullWidth />
              <TextField label="Text Color" value={themeTextColor} onChange={(e) => setThemeTextColor(e.target.value)} placeholder="#111827" fullWidth />
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


