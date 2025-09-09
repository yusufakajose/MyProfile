import { test, expect } from '@playwright/test';

async function registerAndLogin(request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2eref_${unique}`;
  const email = `e2eref_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  return { username: body.username, email: body.email, token: body.token, refreshToken: body.refreshToken };
}

test('refresh via interceptor on 401', async ({ page, request }) => {
  const { username, email, refreshToken } = await registerAndLogin(request);
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: 'invalid.token', refreshToken, user: { username, email } });

  await page.goto('/links');
  await expect(page.getByText('Your Links')).toBeVisible();
});

test('silent reauth on app start with refresh token only', async ({ page, request }) => {
  const { username, email, refreshToken } = await registerAndLogin(request);
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: null, refreshToken, user: { username, email } });

  await page.goto('/analytics');
  await expect(page.getByText('Analytics Overview')).toBeVisible();
});


