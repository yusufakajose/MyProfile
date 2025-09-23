import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router-dom';

jest.mock('../../api/client', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
  }
}));

const client = require('../../api/client').default;
const AnalyticsDashboard = require('../AnalyticsDashboard').default;

const renderDashboard = () => render(
  <MemoryRouter>
    <AnalyticsDashboard />
  </MemoryRouter>
);

const mockEmptyAnalytics = () => {
  client.get.mockImplementation((url) => {
    if (url.includes('/analytics/dashboard/summary')) {
      return Promise.resolve({
        data: {
          totalClicks: 0,
          totalLinks: 0,
          activeLinks: 0,
          inactiveLinks: 0,
          averageClicksPerLink: 0,
          mostPopularLink: null,
        }
      });
    }
    if (url.includes('/analytics/dashboard/timeseries')) {
      return Promise.resolve({
        data: {
          timeseriesData: [],
          totalClicks: 0,
          averageDailyClicks: 0,
          period: '7 days'
        }
      });
    }
    if (url.includes('/analytics/top-links')) {
      return Promise.resolve({ data: { topLinks: [] } });
    }
    if (url.includes('/analytics/referrers')) {
      return Promise.resolve({ data: { referrers: [], period: '7 days' } });
    }
    if (url.includes('/analytics/devices')) {
      return Promise.resolve({ data: { devices: [], period: '7 days' } });
    }
    if (url.includes('/analytics/variants')) {
      return Promise.resolve({ data: { variants: [], period: '7 days' } });
    }
    if (url.includes('/analytics/countries')) {
      return Promise.resolve({ data: { countries: [], period: '7 days' } });
    }
    if (url.includes('/analytics/sources')) {
      return Promise.resolve({ data: { sources: [], period: '7 days' } });
    }
    return Promise.resolve({ data: {} });
  });
};

describe('AnalyticsDashboard states', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  test('renders empty state when no analytics data', async () => {
    mockEmptyAnalytics();
    renderDashboard();
    expect(await screen.findByText('No analytics yet')).toBeInTheDocument();
  });

  test('renders error state and allows retry', async () => {
    // First fetch: all requests fail
    Array.from({ length: 8 }).forEach(() => client.get.mockRejectedValueOnce(new Error('network error')));

    // Subsequent fetch (after retry): resolve with empty analytics
    mockEmptyAnalytics();

    renderDashboard();

    const retryButton = await screen.findByRole('button', { name: /retry loading analytics/i });
    fireEvent.click(retryButton);

    await waitFor(() => expect(client.get).toHaveBeenCalledTimes(8 + 8));
    expect(await screen.findByText('No analytics yet')).toBeInTheDocument();
  });
});


