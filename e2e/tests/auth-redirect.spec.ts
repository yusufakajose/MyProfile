import { test, expect } from '@playwright/test';

test('unauthenticated users are redirected to login', async ({ page }) => {
  await page.goto('/analytics');
  await expect(page).toHaveURL(/\/member-login/);
  await expect(page.getByText('Member Login')).toBeVisible();
});


