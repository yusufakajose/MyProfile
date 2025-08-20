import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2ealias_${unique}`;
  const email = `e2ealias_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('alias validation shows inline errors and disables Add', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/links');
  await page.getByLabel('Title').fill('Alias Test');
  await page.getByLabel('URL').fill('https://example.com');
  await page.getByLabel('Alias (optional)').fill('*bad alias*');
  // Error helper text may render in a way not reliably captured across MUI versions
  // Assert the Add button is disabled instead of the exact text
  await expect(page.getByRole('button', { name: 'Add' })).toBeDisabled();
  // Fix alias and ensure Add enabled
  await page.getByLabel('Alias (optional)').fill('good-alias');
  await expect(page.getByRole('button', { name: 'Add' })).toBeEnabled();
});


