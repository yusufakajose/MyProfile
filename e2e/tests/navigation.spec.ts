import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2enav_${unique}`;
  const email = `e2enav_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('header navigation and user menu', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/analytics');
  await expect(page.getByRole('heading', { name: 'Analytics Overview' })).toBeVisible();

  // Create Link button → Links page
  await page.getByRole('button', { name: 'Create Link' }).click();
  await expect(page).toHaveURL(/\/links/);
  await expect(page.getByText('Your Links')).toBeVisible();

  // Webhooks nav
  await page.getByRole('link', { name: 'Webhooks' }).click();
  await expect(page).toHaveURL(/\/settings\/webhooks/);
  await expect(page.getByLabel('Webhook URL')).toBeVisible();

  // User menu → Profile
  await page.getByLabel(/open user menu/i).click();
  await page.getByRole('menuitem', { name: 'Profile' }).click();
  await expect(page).toHaveURL(/\/settings\/profile/);
  await expect(page.getByRole('heading', { name: 'Profile Settings' })).toBeVisible();

  // Logout
  await page.getByLabel(/open user menu/i).click();
  await page.getByRole('menuitem', { name: /logout/i }).click();
  await expect(page).toHaveURL(/\/member-login/);
  await expect(page.getByText('Member Login')).toBeVisible();
});


