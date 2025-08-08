import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { Container, Box } from '@mui/material';
import AnalyticsDashboard from './components/AnalyticsDashboard';
import Header from './components/Header';
import PublicProfile from './components/PublicProfile';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './components/Login';
import Register from './components/Register';
import LinkManager from './components/LinkManager';
import ProfileSettings from './components/ProfileSettings';
import WebhookSettings from './components/WebhookSettings';
import { AuthProvider } from './context/AuthContext';

function App() {
  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'background.default' }}>
      <Header />
      <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
        <AuthProvider>
          <Routes>
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AnalyticsDashboard />
                </ProtectedRoute>
              }
            />
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
            <Route path="/member-login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/u/:username" element={<PublicProfile />} />
          </Routes>
        </AuthProvider>
      </Container>
    </Box>
  );
}

export default App;
