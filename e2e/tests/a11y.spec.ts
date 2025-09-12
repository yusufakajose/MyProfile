import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.describe('Accessibility', () => {
  test('main pages have no critical a11y violations', async ({ page }) => {
    await page.goto('/');
    // Login page (public)
    let results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations.filter(v => v.impact === 'critical')).toHaveLength(0);

    // Try a protected page path to ensure layout also holds a11y (will redirect to login)
    await page.goto('/links');
    results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations.filter(v => v.impact === 'critical')).toHaveLength(0);
  });

  test('security headers present on frontend and backend', async ({ page, request }) => {
    const resFrontend = await page.request.get('/');
    // On CI we serve with a simple static server that may not include custom headers.
    // Only assert frontend headers locally when CHECK_FRONTEND_HEADERS != '0'.
    if (process.env.CHECK_FRONTEND_HEADERS !== '0') {
      expect(resFrontend.headers()['x-content-type-options']).toBe('nosniff');
      expect(resFrontend.headers()['x-frame-options']).toBe('DENY');
      expect(resFrontend.headers()['content-security-policy']).toContain("default-src 'self'");
      expect(resFrontend.headers()['referrer-policy']).toBeDefined();
    }

    const backendBase = process.env.E2E_BACKEND_URL || 'http://localhost:8080';
    const resBackend = await request.get(`${backendBase}/api/public/doesnotexist`);
    expect(resBackend.headers()['x-content-type-options']).toBe('nosniff');
    expect(resBackend.headers()['x-frame-options']).toBe('DENY');
    expect(resBackend.headers()['content-security-policy']).toContain("default-src 'self'");
    expect(resBackend.headers()['referrer-policy']).toBeDefined();
  });
});


