import { test, expect } from '@playwright/test';

test('unknown route shows 404', async ({ page }) => {
  await page.goto('/some-unknown-route');
  await expect(page.getByText('404')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Go Home' })).toBeVisible();
});


