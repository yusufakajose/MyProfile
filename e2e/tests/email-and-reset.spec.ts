import { test, expect } from '@playwright/test';

async function registerUser(request, username, email, password) {
  const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  await request.post(`${backendBase}/api/auth/register`, { data: { username, email, password } });
}

test('email verification end-to-end via dev token endpoint', async ({ page, request }) => {
  const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `verify_${unique}`;
  const email = `${username}@example.com`;
  const password = 'P@ssw0rd!';
  await registerUser(request, username, email, password);

  // trigger verification email
  await request.post(`${backendBase}/api/auth/initiate-email-verification?username=${username}`);
  const tokenRes = await request.get(`${backendBase}/api/auth/dev/latest-email-verification-token?username=${username}`);
  expect(tokenRes.ok()).toBeTruthy();
  const token = await tokenRes.text();

  await page.goto(`/verify-email?token=${token}`);
  await expect(page.getByText(/Email verified successfully/i)).toBeVisible();

  // Using the same token again should fail
  await page.goto(`/verify-email?token=${token}`);
  await expect(page.getByText(/Verification failed|token expired/i)).toBeVisible();
});

test('password reset end-to-end via dev token endpoint', async ({ page, request }) => {
  const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
  const unique = Date.now();
  const username = `reset_${unique}`;
  const email = `${username}@example.com`;
  const password = 'P@ssw0rd!';
  await registerUser(request, username, email, password);

  await page.goto('/forgot-password');
  await page.getByLabel('Email').fill(email);
  await page.getByRole('button', { name: 'Send Reset Link' }).click();
  await expect(page.getByText(/reset link has been sent/i)).toBeVisible();

  const tokenRes = await request.get(`${backendBase}/api/auth/dev/latest-password-reset-token?email=${email}`);
  expect(tokenRes.ok()).toBeTruthy();
  const token = await tokenRes.text();

  await page.goto(`/reset-password?token=${token}`);
  await page.getByLabel('New Password').fill('NewP@ssw0rd!');
  await page.getByRole('button', { name: 'Set New Password' }).click();
  await expect(page.getByText(/Password has been reset/i)).toBeVisible();

  // Using the same token again should fail
  await page.goto(`/reset-password?token=${token}`);
  await page.getByLabel('New Password').fill('AnotherP@ss1!');
  await page.getByRole('button', { name: 'Set New Password' }).click();
  await expect(page.getByText(/Failed to reset password|invalid or expired/i)).toBeVisible();
});
