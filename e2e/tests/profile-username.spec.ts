import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2euser_${unique}`;
  const email = `e2euser_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('profile username update shows success', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/settings/profile');
  const newName = `updated_${Date.now()}`;
  await page.getByLabel('Username').fill(newName);
  await page.getByRole('button', { name: 'Update Username' }).click();
  await expect(page.getByText('Username updated')).toBeVisible();
});


