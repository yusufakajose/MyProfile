import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import LinkManager from '../../components/LinkManager';
jest.mock('../../api/client', () => ({
  __esModule: true,
  default: {
    defaults: { baseURL: 'http://localhost:3001/api' },
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  }
}));
import client from '../../api/client';

describe('LinkManager component', () => {
  beforeEach(() => {
    client.get.mockImplementation(async (path) => {
      if (path && path.startsWith('/links?')) {
        return { data: { content: [{ id: 1, title: 'T', url: 'https://e.com', description: '', isActive: true, clickCount: 0, alias: '' }], totalPages: 1 } };
      }
      if (path === '/links/tags') return { data: [] };
      if (path === '/links/1/variants') return { data: [] };
      return { data: [] };
    });
    client.post.mockResolvedValue({ data: { id: 2 } });
    client.put.mockResolvedValue({ data: {} });
    client.delete.mockResolvedValue({ data: {} });
  });
  test('creates link when valid and shows toast', async () => {
    render(<LinkManager />);
    const inputs = await screen.findAllByRole('textbox');
    // First two textboxes correspond to Title and URL in the create form
    fireEvent.change(inputs[0], { target: { value: 'New' } });
    fireEvent.change(inputs[1], { target: { value: 'https://example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /add/i }));
    await waitFor(() => expect(screen.getByText(/Link added/i)).toBeInTheDocument());
  });

  test('enters edit mode and saves', async () => {
    render(<LinkManager />);
    const editBtn = await screen.findByRole('button', { name: /edit/i });
    fireEvent.click(editBtn);
    const titleEdit = await screen.findByLabelText('Title');
    fireEvent.change(titleEdit, { target: { value: 'Updated' } });
    const saveBtn = screen.getByLabelText('save');
    fireEvent.click(saveBtn);
    await waitFor(() => expect(screen.getByText(/Link saved/i)).toBeInTheDocument());
  });

  test('optimistic active toggle flips immediately', async () => {
    const client = (await import('../../api/client')).default;
    client.put.mockImplementationOnce(async () => new Promise(resolve => setTimeout(() => resolve({ data: {} }), 50)));
    render(<LinkManager />);
    const toggle = await screen.findByRole('checkbox');
    expect(toggle).toBeChecked();
    fireEvent.click(toggle);
    // optimistic switch should reflect immediately
    expect(toggle).not.toBeChecked();
    await waitFor(() => expect(client.put).toHaveBeenCalled());
  });
});


