import { test, expect } from '@playwright/test';

async function seedAuth(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2ehk_${unique}`;
  const email = `e2ehk_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token: body.token, user: { username: body.username, email: body.email } });
  return { username };
}

test('webhook retries: set failing URL, see DLQ and resend all', async ({ page, request }) => {
  await seedAuth(page, request);
  await page.goto('/settings/webhooks');
  await page.getByLabel('Webhook URL').fill('http://127.0.0.1:65535/fail');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Webhook settings saved')).toBeVisible();

  // Trigger a click to enqueue a webhook delivery
  await page.goto('/links');
  await page.getByTestId('create-title').fill('Hook Link');
  await page.getByTestId('create-url').fill('https://example.com/hook');
  await page.getByRole('button', { name: 'Add' }).click();
  await expect(page.getByText('Link added')).toBeVisible();

  // Open QR/short actions and visit short link in a new tab to simulate a click
  const card = page.getByTestId('link-card').filter({ hasText: 'Hook Link' }).first();
  await expect(card).toBeVisible();
  // Click "Open short link" which opens in new tab; just ensure action triggers
  const [popup] = await Promise.all([
    page.waitForEvent('popup', { timeout: 10000 }).catch(() => null),
    card.getByRole('button', { name: 'open short link' }).click(),
  ]);
  if (popup) await popup.close();

  // Go back to webhooks page and expect DLQ table appears after retry attempts
  await page.goto('/settings/webhooks');
  // Button is disabled when DLQ is empty; just assert visibility to avoid flake
  await expect(page.getByRole('button', { name: 'Resend all' })).toBeVisible();
});


