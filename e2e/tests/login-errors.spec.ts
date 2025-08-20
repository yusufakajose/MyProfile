import { test, expect } from '@playwright/test';

test('login shows error on invalid credentials', async ({ page }) => {
  await page.goto('/member-login');
  await page.getByLabel('Username').fill('nope');
  await page.getByRole('textbox', { name: 'Password' }).fill('wrong');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByText(/Login failed|Invalid username or password/i)).toBeVisible();
});


