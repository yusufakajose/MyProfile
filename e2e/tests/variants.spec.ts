import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2evar_${unique}`;
  const email = `e2evar_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('manage variants: add, save, delete', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/links');
  // Create a base link
  await page.getByLabel('Title').fill('Variant Host Link');
  await page.getByLabel('URL').fill('https://example.com/base');
  await page.getByRole('button', { name: 'Add' }).click();
  await expect(page.getByText('Link added')).toBeVisible();

  // Scope to the card containing our link title
  const card = page.locator('div.MuiCard-root').filter({ hasText: 'Variant Host Link' }).first();
  await card.getByRole('button', { name: /Manage Variants/i }).click();
  // Add variant inside this card
  await card.getByLabel('Title').last().fill('Variant A');
  await card.getByLabel('URL').last().fill('https://example.com/variant-a');
  await card.getByRole('button', { name: 'Add Variant' }).click();
  // Save and then delete from within the card
  await card.getByRole('button', { name: 'Save' }).first().click();
  await card.getByRole('button', { name: 'Delete' }).first().click();
});


