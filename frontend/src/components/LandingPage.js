import React from 'react';
import { Box, Button, Container, Typography, Grid, Card, CardContent, alpha, Stack } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import BarChartIcon from '@mui/icons-material/BarChart';
import QrCodeIcon from '@mui/icons-material/QrCode';
import ScienceIcon from '@mui/icons-material/Science';
import PublicIcon from '@mui/icons-material/Public';
import LinkIcon from '@mui/icons-material/Link';

const LandingPage = () => {
  const navigate = useNavigate();

  const features = [
    {
      icon: <LinkIcon sx={{ fontSize: 40 }} />,
      title: 'Beautiful Link Pages',
      description: 'Create stunning, customizable profile pages with all your important links in one place.'
    },
    {
      icon: <BarChartIcon sx={{ fontSize: 40 }} />,
      title: 'Powerful Analytics',
      description: 'Track clicks, unique visitors, referrers, and devices. Export data to CSV for deeper insights.'
    },
    {
      icon: <QrCodeIcon sx={{ fontSize: 40 }} />,
      title: 'QR Code Generation',
      description: 'Instantly generate QR codes for your links. Perfect for print materials and offline sharing.'
    },
    {
      icon: <ScienceIcon sx={{ fontSize: 40 }} />,
      title: 'A/B Testing',
      description: 'Test multiple destinations with weighted rotation. Optimize your link performance with data.'
    },
    {
      icon: <PublicIcon sx={{ fontSize: 40 }} />,
      title: 'Custom Domains',
      description: 'Use your own domain for professional branding and increased trust.'
    }
  ];

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* Hero Section */}
      <Box
        sx={{
          background: (theme) => theme.palette.mode === 'dark'
            ? `linear-gradient(135deg, ${alpha('#667eea', 0.1)} 0%, ${alpha('#764ba2', 0.1)} 100%)`
            : `linear-gradient(135deg, ${alpha('#667eea', 0.05)} 0%, ${alpha('#764ba2', 0.05)} 100%)`,
          borderBottom: 1,
          borderColor: 'divider',
          py: { xs: 8, md: 12 }
        }}
      >
        <Container maxWidth="lg">
          <Box textAlign="center">
            <Typography
              variant="h1"
              sx={{
                fontSize: { xs: '2.5rem', sm: '3.5rem', md: '4.5rem' },
                fontWeight: 800,
                mb: 2,
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text'
              }}
            >
              Your Links,
              <br />
              Beautifully Organized
            </Typography>
            <Typography
              variant="h5"
              color="text.secondary"
              sx={{ mb: 4, maxWidth: 600, mx: 'auto', fontWeight: 400 }}
            >
              Create a stunning link-in-bio page with powerful analytics, QR codes, and A/B testing.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="center">
              <Button
                variant="contained"
                size="large"
                onClick={() => navigate('/register')}
                sx={{
                  py: 1.5,
                  px: 4,
                  fontSize: '1.1rem',
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #5568d3 0%, #66428e 100%)',
                  }
                }}
              >
                Get Started Free
              </Button>
              <Button
                variant="outlined"
                size="large"
                onClick={() => navigate('/u/demo')}
                sx={{ py: 1.5, px: 4, fontSize: '1.1rem' }}
              >
                View Demo Profile
              </Button>
            </Stack>
          </Box>
        </Container>
      </Box>

      {/* Features Section */}
      <Container maxWidth="lg" sx={{ py: { xs: 8, md: 12 } }}>
        <Typography
          variant="h2"
          textAlign="center"
          sx={{ fontSize: { xs: '2rem', md: '2.75rem' }, fontWeight: 700, mb: 2 }}
        >
          Everything you need
        </Typography>
        <Typography
          variant="h6"
          color="text.secondary"
          textAlign="center"
          sx={{ mb: 6, fontWeight: 400 }}
        >
          Professional tools to grow your audience and track your success
        </Typography>

        <Grid container spacing={4}>
          {features.map((feature, index) => (
            <Grid item xs={12} sm={6} md={4} key={index}>
              <Card
                sx={{
                  height: '100%',
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    transform: 'translateY(-8px)',
                    boxShadow: (theme) => theme.palette.mode === 'dark'
                      ? `0 12px 24px ${alpha('#000', 0.3)}`
                      : `0 12px 24px ${alpha('#667eea', 0.15)}`
                  }
                }}
              >
                <CardContent sx={{ p: 4 }}>
                  <Box
                    sx={{
                      display: 'inline-flex',
                      p: 2,
                      borderRadius: 2,
                      background: (theme) => theme.palette.mode === 'dark'
                        ? alpha('#667eea', 0.1)
                        : alpha('#667eea', 0.08),
                      color: '#667eea',
                      mb: 2
                    }}
                  >
                    {feature.icon}
                  </Box>
                  <Typography variant="h5" gutterBottom sx={{ fontWeight: 600 }}>
                    {feature.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {feature.description}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      {/* CTA Section */}
      <Box
        sx={{
          background: (theme) => theme.palette.mode === 'dark'
            ? `linear-gradient(135deg, ${alpha('#667eea', 0.15)} 0%, ${alpha('#764ba2', 0.15)} 100%)`
            : `linear-gradient(135deg, ${alpha('#667eea', 0.08)} 0%, ${alpha('#764ba2', 0.08)} 100%)`,
          borderTop: 1,
          borderColor: 'divider',
          py: { xs: 6, md: 8 }
        }}
      >
        <Container maxWidth="md">
          <Box textAlign="center">
            <Typography variant="h3" sx={{ fontWeight: 700, mb: 2 }}>
              Ready to get started?
            </Typography>
            <Typography variant="h6" color="text.secondary" sx={{ mb: 4, fontWeight: 400 }}>
              Join thousands of creators sharing their content beautifully
            </Typography>
            <Button
              variant="contained"
              size="large"
              onClick={() => navigate('/register')}
              sx={{
                py: 1.5,
                px: 5,
                fontSize: '1.1rem',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #5568d3 0%, #66428e 100%)',
                }
              }}
            >
              Create Your Page
            </Button>
          </Box>
        </Container>
      </Box>

      {/* Footer */}
      <Box
        sx={{
          borderTop: 1,
          borderColor: 'divider',
          py: 4,
          bgcolor: 'background.paper'
        }}
      >
        <Container maxWidth="lg">
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            justifyContent="space-between"
            alignItems="center"
            spacing={2}
          >
            <Typography variant="body2" color="text.secondary">
              © 2025 LinkGrove. All rights reserved.
            </Typography>
            <Stack direction="row" spacing={3}>
              <Button color="inherit" onClick={() => navigate('/login')} sx={{ textTransform: 'none' }}>
                Login
              </Button>
              <Button color="inherit" onClick={() => navigate('/u/demo')} sx={{ textTransform: 'none' }}>
                Demo
              </Button>
            </Stack>
          </Stack>
        </Container>
      </Box>
    </Box>
  );
};

export default LandingPage;

