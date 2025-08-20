import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2elinks_${unique}`;
  const email = `e2elinks_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
}

test('links: create, edit, toggle, delete', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/links');

  // Create a link
  await page.getByLabel('Title').fill('My test link');
  await page.getByLabel('URL').fill('https://example.com');
  await page.getByRole('button', { name: 'Add' }).click();
  await expect(page.getByText('Link added')).toBeVisible();

  // Find the created card
  const card = page.locator('text=My test link').first();
  await expect(card).toBeVisible();

  // Enter edit mode
  await card.locator('xpath=ancestor::div[contains(@class, "MuiCard-root")]').getByLabel('edit').click();
  await page.getByLabel('Title').fill('My edited link');
  await page.getByLabel('URL').fill('https://example.org');
  await page.getByLabel('save').click();
  await expect(page.getByText('Link saved')).toBeVisible();

  // Toggle active
  await page.getByLabel(/Active|Inactive/).first().click();

  // Delete
  await page.getByLabel('delete').first().click();
  await page.getByRole('button', { name: 'Delete' }).click();
  await expect(page.getByText('Link deleted')).toBeVisible();
});


