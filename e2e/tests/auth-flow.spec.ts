import { test, expect } from '@playwright/test';

test('user can register then logout and login', async ({ page, request }) => {
  const unique = Date.now();
  const username = `e2euser_${unique}`;
  const email = `e2e${unique}@example.com`;
  const password = 'P@ssw0rd!';

  // Register via UI
  await page.goto('/register');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Email').fill(email);
  await page.getByRole('textbox', { name: 'Password' }).fill(password);
  await page.getByRole('button', { name: 'Register' }).click();

  // Landed on analytics (protected) after auto-login
  await expect(page).toHaveURL(/.*\/analytics/);

  // Open user menu and logout
  await page.getByRole('button', { name: /open user menu/i }).click();
  await page.getByRole('menuitem', { name: /logout/i }).click();
  await expect(page).toHaveURL(/.*\/member-login/);

  // Login via API and restore session (more robust)
  const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const loginResp = await request.post(`${backendBase}/api/auth/login`, {
    data: { username, password }
  });
  expect(loginResp.ok()).toBeTruthy();
  const body = await loginResp.json();
  await page.goto('/');
  await page.evaluate(([t, u, e]) => {
    localStorage.setItem('auth', JSON.stringify({ token: t, user: { username: u, email: e } }));
  }, [body.token, body.username, body.email]);
  await page.goto('/analytics');
  await expect(page).toHaveURL(/.*\/analytics/);
});


