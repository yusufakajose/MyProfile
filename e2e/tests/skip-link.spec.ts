import { test, expect } from '@playwright/test';

test('skip link focuses main content', async ({ page }) => {
  await page.goto('/member-login');
  // Tab to reach the hidden skip link (browser might reveal it while focused)
  await page.keyboard.press('Tab');
  // Trigger skip link if focused
  await page.keyboard.press('Enter');
  const activeId = await page.evaluate(() => document.activeElement?.id || '');
  expect(activeId).toBe('main');
});


