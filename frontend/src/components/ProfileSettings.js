import React, { useEffect, useState } from 'react';
import { Box, Card, CardContent, TextField, Button, Typography, Alert, Stack, Avatar } from '@mui/material';
import client from '../api/client';

const ProfileSettings = () => {
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [profileImageUrl, setProfileImageUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await client.get('/user/profile');
        setDisplayName(res.data.displayName || '');
        setBio(res.data.bio || '');
        setProfileImageUrl(res.data.profileImageUrl || '');
      } catch (e) {
        setError('Failed to load profile');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const save = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const res = await client.put('/user/profile', { displayName, bio, profileImageUrl });
      setSuccess('Saved');
      setDisplayName(res.data.displayName || '');
      setBio(res.data.bio || '');
      setProfileImageUrl(res.data.profileImageUrl || '');
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to save');
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


