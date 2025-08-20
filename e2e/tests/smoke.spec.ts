import { test, expect } from '@playwright/test';

test('app loads and shows login or analytics', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/Analytics|LinkGrove/i);
  // Either we see login or we're already authenticated and see analytics
  const loginVisible = await page.getByText('Member Login').isVisible().catch(() => false);
  if (!loginVisible) {
    await expect(page).toHaveURL(/\/(analytics|member-login)/);
  }
});

test('backend health is OK', async ({ request }) => {
  const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const res = await request.get(`${backendBase}/api/auth/health`);
  expect(res.ok()).toBeTruthy();
});


