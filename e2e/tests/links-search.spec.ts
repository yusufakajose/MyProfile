import { test, expect } from '@playwright/test';

async function seedAuthAndLinks(page, request) {
  const base = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `e2esearch_${unique}`;
  const email = `e2esearch_${unique}@example.com`;
  const password = 'P@ssw0rd!';
  await request.post(`${base}/api/auth/register`, { data: { username, email, password } });
  const login = await request.post(`${base}/api/auth/login`, { data: { username, password } });
  const body = await login.json();
  const token = body.token;
  // seed a few links
  for (let i = 1; i <= 3; i++) {
    await request.post(`${base}/api/links`, {
      data: { title: `Seed Link ${i}`, url: `https://example.com/${i}` },
      headers: { Authorization: `Bearer ${token}` }
    });
  }
  await page.addInitScript((auth) => {
    localStorage.setItem('auth', JSON.stringify(auth));
  }, { token, user: { username: body.username, email: body.email } });
}

test('links search and filters', async ({ page, request }) => {
  await seedAuthAndLinks(page, request);
  await page.goto('/links');
  // Search
  await page.getByPlaceholder('Search title/url/description').fill('Seed Link 2');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByText('Seed Link 2')).toBeVisible();
  // Clear
  await page.getByRole('button', { name: 'Clear' }).click();
  await expect(page.getByText('Seed Link 1')).toBeVisible();
});


