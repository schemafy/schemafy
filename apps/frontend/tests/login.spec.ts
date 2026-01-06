import { test, expect } from '@playwright/test';

test.describe('Login Flow', () => {
  test('로그인이 성공적으로 이루어져야 한다', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*\/signin/);

    const email = 'test@example.com';
    const password = 'password123';

    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', password);

    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('http://localhost:3001/');

    await expect(page.getByRole('button', { name: 'Sign In' })).not.toBeVisible();
  });
});
