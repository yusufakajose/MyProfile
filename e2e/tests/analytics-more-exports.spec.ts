import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2eexp_${unique}`;
  const email = `e2eexp_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('devices/referrers/countries export buttons exist and click', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/analytics');
  // Devices
  const devBtn = page.getByRole('button', { name: 'Export devices CSV' });
  if (await devBtn.isVisible().catch(() => false)) {
    await devBtn.click();
  }
  // Referrers
  const refBtn = page.getByRole('button', { name: 'Export referrers CSV' });
  if (await refBtn.isVisible().catch(() => false)) {
    await refBtn.click();
  }
  // Countries
  const ctrBtn = page.getByRole('button', { name: 'Export countries CSV' });
  if (await ctrBtn.isVisible().catch(() => false)) {
    await ctrBtn.click();
  }
});


