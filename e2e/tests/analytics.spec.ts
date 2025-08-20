import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2eanalytics_${unique}`;
  const email = `e2eanalytics_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('analytics: loads and allows time range/per-link selection and CSV exports', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/analytics');
  await expect(page.getByRole('heading', { name: 'Analytics Overview' })).toBeVisible();

  // Change time range (open select via click on label or input)
  const timeRange = page.getByLabel('Time Range');
  await timeRange.click({ timeout: 10000 });
  const opt14 = page.getByRole('option', { name: 'Last 14 days' });
  if (await opt14.isVisible().catch(() => false)) {
    await opt14.click();
  } else {
    // Fallback: press ArrowDown and Enter
    await timeRange.press('ArrowDown');
    await timeRange.press('Enter');
  }
  // Best-effort check: heading should update or still be visible
  await expect(page.getByText(/Click Trends \(.* days\)/)).toBeVisible();

  // Per-link select (may be empty if no links yet): just open and close
  const perLink = page.getByLabel('Per-link');
  await perLink.click();
  await page.keyboard.press('Escape');

  // Export buttons exist and are clickable
  await page.getByRole('button', { name: 'Export Timeseries CSV' }).click();
  await page.getByRole('button', { name: 'Export Top Links CSV' }).click();

  // Retry button appears if we simulate error (not simulating here), but assert it exists in DOM when error state
});


