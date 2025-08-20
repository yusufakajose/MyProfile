import { test, expect } from '@playwright/test';

test('skip link navigates to #main', async ({ page }) => {
  await page.goto('/member-login');
  // Programmatically activate the skip link to avoid hidden click restrictions
  await page.evaluate(() => {
    document.querySelector('a[href="#main"]')?.click();
  });
  await expect(page).toHaveURL(/#main$/);
});


