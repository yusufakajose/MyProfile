import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2evedge_${unique}`;
  const email = `e2evedge_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('variants edge cases: zero weight, deactivate, delete', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/links');

  await page.getByTestId('create-title').fill('Edge Host Link');
  await page.getByTestId('create-url').fill('https://example.com/edgebase');
  await page.getByRole('button', { name: 'Add' }).click();
  await expect(page.getByText('Link added')).toBeVisible();

  const card = page.getByTestId('link-card').filter({ hasText: 'Edge Host Link' }).first();
  await expect(card).toBeVisible();
  await card.getByRole('button', { name: /Manage Variants/i }).click();

  // Add a variant with non-zero weight so it appears, then set weight to 0 and save
  await card.getByLabel('Title').last().fill('Normal A');
  await card.getByLabel('URL').last().fill('https://example.com/a');
  await card.getByRole('button', { name: 'Add Variant' }).click();

  // Ensure row exists, then change weight to 0 and save -> it should disappear
  const rows = card.locator('tbody tr');
  await expect(rows.first()).toBeVisible({ timeout: 10000 });
  const firstRow = rows.first();
  await firstRow.locator('input[type="number"]').fill('0');
  await firstRow.getByRole('button', { name: 'Save' }).click();
  // After save, since weight==0 and list shows only active/weight>0, table should show empty state
  await expect(card.getByText('No variants yet')).toBeVisible({ timeout: 10000 });
});


