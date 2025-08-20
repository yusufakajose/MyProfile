import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2eqr_${unique}`;
  const email = `e2eqr_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('QR dialog shows and controls are enabled', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/links');
  // Create a link to have a card
  await page.getByLabel('Title').fill('QR Test Link');
  await page.getByLabel('URL').fill('https://example.com/qr');
  await page.getByRole('button', { name: 'Add' }).click();
  await expect(page.getByText('Link added')).toBeVisible();

  // Open QR dialog from the first card
  await page.getByLabel('get qr').first().click();
  await expect(page.getByRole('heading', { name: 'Download QR code' })).toBeVisible();
  await expect(page.getByLabel('Format')).toBeVisible();
  await expect(page.getByLabel('Error correction')).toBeVisible();
  await expect(page.getByLabel('Size (px)')).toBeVisible();
  await expect(page.getByLabel('Margin')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Download' })).toBeEnabled();
  await page.getByRole('button', { name: 'Close' }).click();
});


