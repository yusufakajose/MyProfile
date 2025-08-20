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

  // Login via API and restore session
  const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const loginResp = await request.post(`${backendBase}/api/auth/login`, {
    data: { username, password }
  });
  expect(loginResp.ok()).toBeTruthy();
  const body = await loginResp.json();
  // Ensure the app sees auth at initial load by injecting before navigation
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
  await page.goto('/analytics');
  await expect(page).toHaveURL(/.*\/analytics/);
});


