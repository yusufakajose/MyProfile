import { test, expect } from '@playwright/test';

test('theme toggle switches modes', async ({ page }) => {
  await page.goto('/member-login');
  const initialBg = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
  await page.getByLabel(/toggle color mode/i).click();
  const toggledBg = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
  expect(toggledBg).not.toEqual(initialBg);
});


