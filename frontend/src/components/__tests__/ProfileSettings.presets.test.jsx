import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';

jest.mock('../../api/client', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    put: jest.fn()
  }
}));

jest.mock('../../context/AuthContext', () => ({
  useAuth: () => ({ login: jest.fn(), user: { username: 'jane' }, token: 'x' })
}));

const client = require('../../api/client').default;
const ProfileSettings = require('../ProfileSettings').default;

describe('ProfileSettings theming presets', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  const renderWithData = async (profileOverrides = {}) => {
    client.get.mockResolvedValueOnce({
      data: {
        displayName: 'Jane',
        bio: 'Bio',
        profileImageUrl: '',
        username: 'jane',
        emailVerified: true,
        themePrimaryColor: '',
        themeAccentColor: '',
        themeBackgroundColor: '',
        themeTextColor: '',
        ...profileOverrides
      }
    });
    client.put.mockResolvedValue({ data: {} });
    render(<ProfileSettings />);
    await screen.findByText('Profile Settings');
  };

  test('selecting preset updates color inputs and flags preset', async () => {
    await renderWithData();
    const lightButton = screen.getByRole('button', { name: /light/i });
    fireEvent.click(lightButton);
    expect(screen.getByLabelText(/Primary Color/i)).toHaveValue('#1976d2');
    expect(screen.getByLabelText(/Accent Color/i)).toHaveValue('#1f2937');
    expect(screen.getByLabelText(/Background Color/i)).toHaveValue('#ffffff');
    expect(screen.getByLabelText(/Text Color/i)).toHaveValue('#111827');
  });

  test('manual color change switches preset to custom', async () => {
    await renderWithData();
    fireEvent.click(screen.getByRole('button', { name: /sunset/i }));
    expect(screen.getByRole('button', { name: /sunset/i })).toHaveAttribute('aria-pressed', 'true');
    const primaryInput = screen.getByLabelText(/Primary Color/i);
    fireEvent.change(primaryInput, { target: { value: '#123456' } });
    await waitFor(() => expect(screen.getByRole('button', { name: /custom/i })).toHaveAttribute('aria-pressed', 'true'));
  });
});


