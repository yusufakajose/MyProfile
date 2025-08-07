import React, { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Avatar,
  Typography,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  Alert,
  Grid,
  Chip,
  Skeleton,
  Button,
} from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import LinkIcon from '@mui/icons-material/Link';
import axios from 'axios';

const API_BASE = 'http://localhost:8080';

const ProfileHeaderSkeleton = () => (
  <Box sx={{ py: 6, textAlign: 'center' }}>
    <Skeleton variant="circular" width={96} height={96} sx={{ mx: 'auto', mb: 2 }} />
    <Skeleton variant="text" width={220} height={36} sx={{ mx: 'auto' }} />
    <Skeleton variant="text" width={420} height={20} sx={{ mx: 'auto', mt: 1 }} />
  </Box>
);

const PublicProfile = () => {
  const { username } = useParams();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchProfile = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`${API_BASE}/api/public/${username}`);
      setProfile(res.data);
    } catch (err) {
      setError('Profile not found');
    } finally {
      setLoading(false);
    }
  }, [username]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const handleLinkClick = async (link) => {
    axios.post(`${API_BASE}/api/public/click/${link.id}`).catch(() => {});
    window.open(link.url, '_blank', 'noopener,noreferrer');
  };

  if (loading) {
    return (
      <Box>
        <ProfileHeaderSkeleton />
        <Box maxWidth={720} mx="auto" px={2} mt={2}>
          {[1,2,3].map((k) => (
            <Card key={k} sx={{ mb: 2 }}>
              <CardContent>
                <Skeleton variant="text" width="40%" height={28} />
                <Skeleton variant="text" width="70%" />
              </CardContent>
            </Card>
          ))}
        </Box>
      </Box>
    );
  }

  if (error || !profile) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
        <Alert severity="error">{error || 'Something went wrong'}</Alert>
      </Box>
    );
  }

  return (
    <Box>
      {/* Compact header (banner removed) */}
      <Box sx={{ textAlign: 'center', pt: 6, pb: 2 }}>
        <Avatar
          src={profile.profileImageUrl || undefined}
          alt={profile.displayName || profile.username}
          sx={{
            width: 104,
            height: 104,
            mb: 2,
            display: 'block',
            mx: 'auto',
            boxShadow: '0 8px 24px rgba(2,6,23,0.18)'
          }}
        />
        <Typography variant="h5" fontWeight={800} gutterBottom>
          {profile.displayName || `@${profile.username}`}
        </Typography>
        {profile.bio && (
          <Typography variant="body1" color="text.secondary">
            {profile.bio}
          </Typography>
        )}
        <Chip label={`${profile.links?.length || 0} links`} size="small" sx={{ mt: 1 }} />
      </Box>

      {/* Links */}
      <Box maxWidth={720} mx="auto" px={2} mt={2}>
        <Grid container spacing={2}>
          {(profile.links || []).sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)).map((link) => (
            <Grid key={link.id} item xs={12}>
              <Card elevation={3} sx={{ borderRadius: 16 }}>
                <CardActionArea onClick={() => handleLinkClick(link)}>
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Box>
                        <Typography variant="h6" fontWeight={700}>{link.title}</Typography>
                        {link.description && (
                          <Typography variant="body2" color="text.secondary">
                            {link.description}
                          </Typography>
                        )}
                        <Box display="flex" alignItems="center" gap={1} mt={0.5}>
                          <LinkIcon fontSize="small" color="primary" />
                          <Typography variant="caption" color="primary">
                            {link.url}
                          </Typography>
                        </Box>
                      </Box>
                      <Button variant="contained" endIcon={<OpenInNewIcon />} sx={{ borderRadius: 999 }}>
                        Open
                      </Button>
                    </Box>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>
    </Box>
  );
};

export default PublicProfile;
