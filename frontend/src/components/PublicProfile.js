import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Avatar,
  Typography,
  Card,
  CardActionArea,
  CardContent,
  Alert,
  Grid,
  Chip,
  Skeleton,
  Button,
  IconButton,
  Tooltip,
  Menu,
  MenuItem,
  Snackbar,
} from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import LinkIcon from '@mui/icons-material/Link';
import ShareIcon from '@mui/icons-material/Share';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import FacebookIcon from '@mui/icons-material/Facebook';
import TwitterIcon from '@mui/icons-material/Twitter';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import client from '../api/client';

const ProfileHeaderSkeleton = () => (
  <Box sx={{ py: 6, textAlign: 'center' }}>
    <Skeleton variant="circular" width={96} height={96} sx={{ mx: 'auto', mb: 2 }} />
    <Skeleton variant="text" width={220} height={36} sx={{ mx: 'auto' }} />
    <Skeleton variant="text" width={420} height={20} sx={{ mx: 'auto', mt: 1 }} />
  </Box>
);

const Favicon = ({ url, size = 20 }) => {
  const host = useMemo(() => {
    try { return new URL(url).hostname; } catch { return ''; }
  }, [url]);
  const [src, setSrc] = useState(host ? `https://icons.duckduckgo.com/ip3/${host}.ico` : null);
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    setFailed(false);
    setSrc(host ? `https://icons.duckduckgo.com/ip3/${host}.ico` : null);
  }, [host]);
  if (!host || failed || !src) return <LinkIcon sx={{ fontSize: size, color: 'action.active' }} />;
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

const PublicProfile = () => {
  const { username } = useParams();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [shareAnchorEl, setShareAnchorEl] = useState(null);
  const shareMenuOpen = Boolean(shareAnchorEl);
  const [linkShare, setLinkShare] = useState({ anchorEl: null, link: null });
  const [toast, setToast] = useState({ open: false, message: '' });

  const fetchProfile = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await client.get(`/public/${username}`);
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

  const redirectOrigin = useMemo(() => {
    const base = client.defaults?.baseURL || '';
    try {
      const parsed = new URL(base);
      return parsed.origin;
    } catch {
      const origin = window.location.origin || '';
      return origin.replace(':3000', ':8080');
    }
  }, []);

  const profileUrl = useMemo(() => {
    const origin = window.location.origin || '';
    return `${origin}/u/${encodeURIComponent(username)}`;
  }, [username]);

  const openShareWindow = (url) => {
    // Open in a new tab (no sizing specs, so browsers use a tab not a popup)
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const shareMessage = (url) => `Check out this Linkgrove! - ${url}`;

  const buildTargets = (url) => {
    const msg = shareMessage(url);
    return {
      x: `https://twitter.com/intent/tweet?text=${encodeURIComponent(msg)}`,
      facebook: `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}&quote=${encodeURIComponent(msg)}`,
      whatsapp: `https://api.whatsapp.com/send?text=${encodeURIComponent(msg)}`,
      linkedin: `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`,
    };
  };

  const handleProfileShareClick = async (e) => {
    const text = shareMessage(profileUrl);
    if (navigator.share) {
      try {
        await navigator.share({ title: 'Linkgrove', text, url: profileUrl });
        return;
      } catch {}
    }
    setShareAnchorEl(e.currentTarget);
  };

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      setToast({ open: true, message: 'Link copied' });
    } catch {
      setToast({ open: true, message: 'Copied' });
    }
  };

  const shortUrlFor = (link) => link.alias
    ? `${redirectOrigin}/r/a/${encodeURIComponent(link.alias)}`
    : `${redirectOrigin}/r/${link.id}`;

  const handleLinkClick = async (link) => {
    window.open(shortUrlFor(link), '_blank', 'noopener,noreferrer');
  };

  const openLinkShareMenu = (e, link) => {
    if (navigator.share) {
      const url = shortUrlFor(link);
      const text = shareMessage(url);
      navigator.share({ title: 'Linkgrove', text, url }).catch(() => {});
      return;
    }
    setLinkShare({ anchorEl: e.currentTarget, link });
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

  const primary = profile.themePrimaryColor || '#1976d2';
  const accent = profile.themeAccentColor || '#ff4081';
  const bg = profile.themeBackgroundColor || '#ffffff';
  const text = profile.themeTextColor || '#111827';

  return (
    <Box sx={{ backgroundColor: bg, color: text, minHeight: '100vh' }}>
      {/* Compact header (banner removed) */}
      <Box sx={{ textAlign: 'center', pt: 6, pb: 2, position: 'relative' }}>
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
        <Box sx={{ mt: 2 }}>
          <Button variant="outlined" startIcon={<ShareIcon />} onClick={handleProfileShareClick}>
            Share profile
          </Button>
        </Box>
      </Box>

      {/* Profile share menu */}
      <Menu
        anchorEl={shareAnchorEl}
        open={shareMenuOpen}
        onClose={() => setShareAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem onClick={() => { openShareWindow(buildTargets(profileUrl).x); setShareAnchorEl(null); }}>
          <TwitterIcon fontSize="small" style={{ marginRight: 8 }} /> Share on X/Twitter
        </MenuItem>
        <MenuItem onClick={() => { openShareWindow(buildTargets(profileUrl).facebook); setShareAnchorEl(null); }}>
          <FacebookIcon fontSize="small" style={{ marginRight: 8 }} /> Share on Facebook
        </MenuItem>
        <MenuItem onClick={() => { openShareWindow(buildTargets(profileUrl).whatsapp); setShareAnchorEl(null); }}>
          <WhatsAppIcon fontSize="small" style={{ marginRight: 8 }} /> Share on WhatsApp
        </MenuItem>
        <MenuItem onClick={() => { openShareWindow(buildTargets(profileUrl).linkedin); setShareAnchorEl(null); }}>
          <LinkedInIcon fontSize="small" style={{ marginRight: 8 }} /> Share on LinkedIn
        </MenuItem>
        <MenuItem onClick={() => { copyToClipboard(profileUrl); setShareAnchorEl(null); }}>
          Copy profile link
        </MenuItem>
      </Menu>

      {/* Links */}
      <Box maxWidth={720} mx="auto" px={2} mt={2}>
        <Grid container spacing={2}>
          {(profile.links || []).sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)).map((link) => (
            <Grid key={link.id} item xs={12}>
              <Card elevation={3} sx={{ borderRadius: 16, backgroundColor: '#fff', boxShadow: '0 8px 18px rgba(2,6,23,0.05), 0 2px 6px rgba(2,6,23,0.04)', transition: 'transform 120ms ease, box-shadow 120ms ease', '&:hover': { transform: 'translateY(-2px)', boxShadow: '0 12px 28px rgba(2,6,23,0.07), 0 4px 12px rgba(2,6,23,0.06)' }, '&:active': { transform: 'translateY(0)', boxShadow: '0 8px 18px rgba(2,6,23,0.05), 0 2px 6px rgba(2,6,23,0.04)' } }}>
                <CardActionArea onClick={() => handleLinkClick(link)} sx={{ borderRadius: 2 }}>
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Box>
                        <Box display="flex" alignItems="center" gap={1}>
                          <Favicon url={link.url} size={20} />
                          <Typography variant="h6" fontWeight={700} sx={{ color: primary }}>{link.title}</Typography>
                        </Box>
                        {link.description && (
                          <Typography variant="body2" color="text.secondary">
                            {link.description}
                          </Typography>
                        )}
                        <Box display="flex" alignItems="center" gap={1} mt={0.5}>
                          <LinkIcon fontSize="small" sx={{ color: accent }} />
                          <Typography variant="caption" sx={{ color: accent }}>
                            {link.url}
                          </Typography>
                        </Box>
                      </Box>
                      <Box display="flex" alignItems="center" gap={1}>
                        <Tooltip title="Share link">
                          <IconButton size="small" onClick={(e) => { e.stopPropagation(); openLinkShareMenu(e, link); }}>
                            <ShareIcon />
                          </IconButton>
                        </Tooltip>
                        <Button variant="contained" endIcon={<OpenInNewIcon />} sx={{ borderRadius: 999, backgroundColor: primary }}>
                          Open
                        </Button>
                      </Box>
                    </Box>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>

      {/* Per-link share menu */}
      <Menu
        anchorEl={linkShare.anchorEl}
        open={Boolean(linkShare.anchorEl)}
        onClose={() => setLinkShare({ anchorEl: null, link: null })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        {linkShare.link && (
          <>
            <MenuItem onClick={() => { openShareWindow(buildTargets(shortUrlFor(linkShare.link)).x); setLinkShare({ anchorEl: null, link: null }); }}>
              <TwitterIcon fontSize="small" style={{ marginRight: 8 }} /> Share on X/Twitter
            </MenuItem>
            <MenuItem onClick={() => { openShareWindow(buildTargets(shortUrlFor(linkShare.link)).facebook); setLinkShare({ anchorEl: null, link: null }); }}>
              <FacebookIcon fontSize="small" style={{ marginRight: 8 }} /> Share on Facebook
            </MenuItem>
            <MenuItem onClick={() => { openShareWindow(buildTargets(shortUrlFor(linkShare.link)).whatsapp); setLinkShare({ anchorEl: null, link: null }); }}>
              <WhatsAppIcon fontSize="small" style={{ marginRight: 8 }} /> Share on WhatsApp
            </MenuItem>
            <MenuItem onClick={() => { openShareWindow(buildTargets(shortUrlFor(linkShare.link)).linkedin); setLinkShare({ anchorEl: null, link: null }); }}>
              <LinkedInIcon fontSize="small" style={{ marginRight: 8 }} /> Share on LinkedIn
            </MenuItem>
            <MenuItem onClick={() => { copyToClipboard(shortUrlFor(linkShare.link)); setLinkShare({ anchorEl: null, link: null }); }}>
              Copy link
            </MenuItem>
          </>
        )}
      </Menu>

      {/* Toast */}
      <Snackbar
        open={toast.open}
        autoHideDuration={2000}
        onClose={() => setToast({ open: false, message: '' })}
        message={toast.message}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </Box>
  );
};

export default PublicProfile;
