import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2emobile_${unique}`;
  const email = `e2emobile_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('mobile header menu shows nav items', async ({ page, request }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await seedAuth(page, request);
  await page.goto('/analytics');
  await page.getByLabel('open navigation menu').click();
  await expect(page.getByRole('link', { name: 'Analytics' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Links' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Webhooks' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create Link' })).toBeVisible();
});


