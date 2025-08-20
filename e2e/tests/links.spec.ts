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
  await page.getByTestId('create-title').fill('My test link');
  await page.getByTestId('create-url').fill('https://example.com');
  await page.getByRole('button', { name: 'Add' }).click();
  await expect(page.getByText('Link added')).toBeVisible();

  // Find the created card (stable test id on card)
  const hostCard = page.getByTestId('link-card').filter({ hasText: 'My test link' }).first();
  await expect(hostCard).toBeVisible();

  // Enter edit mode and wait for inputs (use global selectors since the card text disappears)
  await hostCard.getByTestId('edit-button').click();
  await expect(page.getByTestId('edit-title')).toBeVisible();
  await page.getByTestId('edit-title').fill('My edited link');
  await page.getByTestId('edit-url').fill('https://example.org');
  // Ensure alias is empty to satisfy backend optional null vs empty rules
  const aliasInput = page.getByLabel('Alias (optional)');
  if (await aliasInput.isVisible().catch(() => false)) {
    await aliasInput.fill('');
  }
  await expect(page.getByTestId('save-button')).toBeEnabled();
  await page.getByTestId('save-button').click();

  // Re-find updated card by new title, then toggle active within it
  const updatedCard = page.getByTestId('link-card').filter({ hasText: 'My edited link' }).first();
  await expect(updatedCard).toBeVisible();
  await updatedCard.getByTestId('active-switch').click();

  // Delete
  await updatedCard.getByTestId('delete-button').click();
  await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click();
  await expect(updatedCard).toHaveCount(0);
});


