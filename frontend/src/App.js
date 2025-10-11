import React, { useEffect } from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import { Container, Box } from '@mui/material';
import AnalyticsDashboard from './components/AnalyticsDashboard';
import Header from './components/Header';
import PublicProfile from './components/PublicProfile';
import NotFound from './components/NotFound';
import PublicLayout from './components/PublicLayout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './components/Login';
import Register from './components/Register';
import LinkManager from './components/LinkManager';
import ProfileSettings from './components/ProfileSettings';
import WebhookSettings from './components/WebhookSettings';
import SessionSettings from './components/SessionSettings';
import VerifyEmailPage from './components/VerifyEmailPage';
import ForgotPasswordPage from './components/ForgotPasswordPage';
import ResetPasswordPage from './components/ResetPasswordPage';
import LandingPage from './components/LandingPage';
import { AuthProvider } from './context/AuthContext';

function App() {
  const location = useLocation();
  const isPublicRoute = location.pathname.startsWith('/u/');
  const isLandingPage = location.pathname === '/';
  const appBackground = isPublicRoute ? 'transparent' : 'background.default';
  useEffect(() => {
    const path = location.pathname;
    let title = 'LinkGrove';
    if (path === '/') title = 'LinkGrove - Your Links, Beautifully Organized';
    else if (path.startsWith('/analytics')) title = 'Analytics · LinkGrove';
    else if (path.startsWith('/links')) title = 'Links · LinkGrove';
    else if (path.startsWith('/settings/profile')) title = 'Profile Settings · LinkGrove';
    else if (path.startsWith('/settings/webhooks')) title = 'Webhook Settings · LinkGrove';
    else if (path.startsWith('/settings/sessions')) title = 'Sessions · LinkGrove';
    else if (path.startsWith('/verify-email')) title = 'Verify Email · LinkGrove';
    else if (path.startsWith('/forgot-password')) title = 'Forgot Password · LinkGrove';
    else if (path.startsWith('/reset-password')) title = 'Reset Password · LinkGrove';
    else if (path.startsWith('/member-login') || path.startsWith('/login')) title = 'Login · LinkGrove';
    else if (path.startsWith('/register')) title = 'Register · LinkGrove';
    else if (path.startsWith('/u/')) title = 'Profile · LinkGrove';
    document.title = title;
  }, [location.pathname]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [location.pathname]);
  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: appBackground }}>
      <AuthProvider>
        {!isPublicRoute && !isLandingPage && <Header />}
        {/* Sticky AppBar doesn't need extra Toolbar spacer or Divider */}
        {isPublicRoute ? (
          <PublicLayout>
            <Routes>
              <Route path="/u/:username" element={<PublicProfile />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </PublicLayout>
        ) : isLandingPage ? (
          <Routes>
            <Route path="/" element={<LandingPage />} />
          </Routes>
        ) : (
          <Container id="main" maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
            <Routes>
            <Route
              path="/analytics"
              element={
                <ProtectedRoute>
                  <AnalyticsDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/links"
              element={
                <ProtectedRoute>
                  <LinkManager />
                </ProtectedRoute>
              }
            />
            <Route
              path="/settings/profile"
              element={
                <ProtectedRoute>
                  <ProfileSettings />
                </ProtectedRoute>
              }
            />
            <Route
              path="/settings/webhooks"
              element={
                <ProtectedRoute>
                  <WebhookSettings />
                </ProtectedRoute>
              }
            />
            <Route
              path="/settings/sessions"
              element={
                <ProtectedRoute>
                  <SessionSettings />
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<Login />} />
            <Route path="/member-login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="*" element={<NotFound />} />
            </Routes>
          </Container>
        )}
      </AuthProvider>
    </Box>
  );
}

export default App;
