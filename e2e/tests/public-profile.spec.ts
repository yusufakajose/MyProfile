import { test, expect } from '@playwright/test';

async function seedUserAndLink(request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2epub_${unique}`;
  const email = `e2epub_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  const token = body.token;
  // Create a link for the public profile
  const res = await request.post(`${base}/api/links`, {
    data: { title: 'Public Test Link', url: 'https://example.com/profile' },
    headers: { Authorization: `Bearer ${token}` }
  });
  expect(res.ok()).toBeTruthy();
  return { username };
}

test('public profile shows links and share menu works', async ({ page, request }) => {
  const { username } = await seedUserAndLink(request);
  await page.goto(`/u/${username}`);
  await expect(page.getByRole('button', { name: 'Share profile' })).toBeVisible();
  await expect(page.getByText('Public Test Link')).toBeVisible();

  // Open share menu and assert options
  await page.getByRole('button', { name: 'Share profile' }).click();
  await expect(page.getByRole('menuitem', { name: /Share on X\/Twitter/i })).toBeVisible();
  await expect(page.getByRole('menuitem', { name: /Copy profile link/i })).toBeVisible();
});


