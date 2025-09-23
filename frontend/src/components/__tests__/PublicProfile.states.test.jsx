import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

// Mock API client
jest.mock('../../api/client', () => ({
  __esModule: true,
  default: {
    defaults: { baseURL: '/api' },
    get: jest.fn(),
  }
}));

const client = require('../../api/client').default;
const PublicProfile = require('../PublicProfile').default;

describe('PublicProfile states', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  test('renders empty state when profile has no links', async () => {
    client.get.mockResolvedValueOnce({ data: { username: 'alice', displayName: 'Alice', bio: '', links: [] } });
    render(
      <MemoryRouter initialEntries={["/u/alice"]}>
        <Routes>
          <Route path="/u/:username" element={<PublicProfile />} />
        </Routes>
      </MemoryRouter>
    );

    // Wait for empty state to appear
    expect(await screen.findByText('No links yet')).toBeInTheDocument();
  });

  test('renders error state and retries fetch', async () => {
    client.get.mockRejectedValueOnce(new Error('404'));
    // On retry, succeed
    client.get.mockResolvedValueOnce({ data: { username: 'bob', displayName: 'Bob', bio: '', links: [] } });

    render(
      <MemoryRouter initialEntries={["/u/bob"]}>
        <Routes>
          <Route path="/u/:username" element={<PublicProfile />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(client.get).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('Profile not found')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    await waitFor(() => expect(client.get).toHaveBeenCalledTimes(2));
    expect(await screen.findByText('No links yet')).toBeInTheDocument();
  });
});


