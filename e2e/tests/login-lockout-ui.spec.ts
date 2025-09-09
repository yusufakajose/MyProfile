import { test, expect } from '@playwright/test';

test('lockout messaging is surfaced on UI', async ({ page, request }) => {
  const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `lockui_${unique}`;
  const email = `lockui_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${backendBase}/api/auth/register`, { data: { username, email, password } });

  await page.goto('/member-login');
  for (let i = 0; i < 6; i++) {
    await page.getByLabel('Username').fill(username);
    await page.getByRole('textbox', { name: 'Password' }).fill('wrong');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.waitForTimeout(150);
  }
  await expect(page.getByText(/Too many failed attempts|Try again later/i)).toBeVisible({ timeout: 5000 });
});


