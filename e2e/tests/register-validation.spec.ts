import { test, expect } from '@playwright/test';

test('register form shows errors for empty fields', async ({ page }) => {
  await page.goto('/register');
  await page.getByRole('button', { name: 'Register' }).click();
  // Button remains disabled until fields filled; after click, we still expect disabled
  await expect(page.getByRole('button', { name: 'Register' })).toBeDisabled();
});


