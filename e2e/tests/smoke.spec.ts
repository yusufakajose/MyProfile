import { test, expect } from '@playwright/test';

test('app loads and shows login page', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/Analytics|LinkGrove/i);
  // Redirects to login due to ProtectedRoute
  await expect(page.getByText('Member Login')).toBeVisible();
});

test('backend health is OK', async ({ request }) => {
  const res = await request.get('/api/auth/health');
  expect(res.ok()).toBeTruthy();
});


