import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import App from '../App';

jest.mock('../components/ProtectedRoute', () => ({ children }) => <>{children}</>);

jest.mock('../context/AuthContext', () => ({
  AuthProvider: ({ children }) => <>{children}</>
}));

jest.mock('../components/AnalyticsDashboard', () => () => <div>Analytics</div>);
jest.mock('../components/LinkManager', () => () => <div>Links</div>);
jest.mock('../components/ProfileSettings', () => () => <div>Profile Settings</div>);
jest.mock('../components/WebhookSettings', () => () => <div>Webhook Settings</div>);
jest.mock('../components/Login', () => () => <div>Login</div>);
jest.mock('../components/Register', () => () => <div>Register</div>);
jest.mock('../components/PublicProfile', () => () => <div>Public Profile</div>);
jest.mock('../components/Header', () => () => <div>Header</div>);

describe('App route smoke rendering', () => {
  test('renders Links on /links', () => {
    // jsdom does not implement scrollTo
    window.scrollTo = jest.fn();
    render(
      <MemoryRouter initialEntries={["/links"]}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByText('Links')).toBeInTheDocument();
  });

  test('renders Analytics on /', () => {
    window.scrollTo = jest.fn();
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByText('Analytics')).toBeInTheDocument();
  });

  test('renders Public Profile on /u/:username', () => {
    window.scrollTo = jest.fn();
    render(
      <MemoryRouter initialEntries={["/u/john"]}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByText('Public Profile')).toBeInTheDocument();
  });
});


