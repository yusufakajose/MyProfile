import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2edl_${unique}`;
  const email = `e2edl_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('analytics CSV exports trigger downloads', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/analytics');
  const d1 = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export Timeseries CSV' }).click();
  const download1 = await d1;
  const name1 = await download1.suggestedFilename();
  expect(name1).toMatch(/analytics_timeseries_/);

  const topLinksButton = page.getByRole('button', { name: 'Export Top Links CSV' });
  if (await topLinksButton.isVisible().catch(() => false)) {
    const d2 = page.waitForEvent('download');
    await topLinksButton.click();
    await d2;
  }
});


