import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2ehook_${unique}`;
  const email = `e2ehook_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('webhooks: save url and resend DLQ button exists', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/settings/webhooks');
  await page.getByLabel('Webhook URL').fill('http://localhost:7777/h');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Webhook settings saved')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Resend all' })).toBeVisible();
});


