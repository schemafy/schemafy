import { test, expect } from '@playwright/test';

test.describe('Login Flow', () => {
  test('로그인이 성공적으로 이루어져야 한다', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*\/signin/);

    //TODO: frontend 하단에 env 파일 생성 후 등록
    const email = process.env.TEST_EMAIL ?? 'test@example.com';
    const password = process.env.TEST_PASSWORD ?? 'password123';

    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', password);

    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('http://localhost:3000/');

    await expect(page.getByRole('button', { name: 'Sign In' })).not.toBeVisible();
  });
});
