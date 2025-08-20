import { test, expect } from '@playwright/test';

async function seedAuthAndManyLinks(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2epage_${unique}`;
  const email = `e2epage_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  const token = body.token;
  for (let i = 1; i <= 25; i++) {
    await request.post(`${base}/api/links`, {
      data: { title: `Paged Link ${i}`, url: `https://example.com/p${i}` },
      headers: { Authorization: `Bearer ${token}` }
    });
  }
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token, user: { username: body.username, email: body.email } });
}

test('links pagination shows next page and can navigate', async ({ page, request }) => {
  await seedAuthAndManyLinks(page, request);
  await page.goto('/links');
  // Expect next page control and navigate
  const nextButton = page.getByRole('button', { name: /Go to next page/i });
  await expect(nextButton).toBeVisible();
  await nextButton.click();
  // Alternatively jump to page 2 if explicit option exists
  const page2 = page.getByRole('button', { name: /Go to page 2/i });
  if (await page2.isVisible().catch(() => false)) {
    await page2.click();
  }
});


