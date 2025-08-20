import { test, expect } from '@playwright/test';

test('register button disabled until fields are filled', async ({ page }) => {
  await page.goto('/register');
  await expect(page.getByRole('button', { name: 'Register' })).toBeDisabled();
});


