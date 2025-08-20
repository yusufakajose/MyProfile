import { test, expect } from '@playwright/test';

async function seedAuthAndTaggedLinks(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2etag_${unique}`;
  const email = `e2etag_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  const token = body.token;
  const mk = async (title, url, tags) => request.post(`${base}/api/links`, {
    data: { title, url, tags }, headers: { Authorization: `Bearer ${token}` }
  });
  await mk('Tagged A', 'https://example.com/a', ['alpha']);
  await mk('Tagged B', 'https://example.com/b', ['beta']);
  await mk('Tagged AB', 'https://example.com/ab', ['alpha','beta']);
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token, user: { username: body.username, email: body.email } });
}

test('links tag filters narrow results', async ({ page, request }) => {
  await seedAuthAndTaggedLinks(page, request);
  await page.goto('/links');
  // Click alpha tag button
  await page.getByRole('button', { name: 'alpha' }).click();
  await expect(page.getByText('Tagged A')).toBeVisible();
  await expect(page.getByText('Tagged B')).toHaveCount(0);
});


