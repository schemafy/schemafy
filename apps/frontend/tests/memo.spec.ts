import { test, expect } from '@playwright/test';

test.describe('Memo Flow', () => {
  // 메모 서버 등록을 위한 로그인 전처리
  test.beforeEach(async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*\/signin/);

    const email = process.env.TEST_EMAIL ?? 'test@example.com';
    const password = process.env.TEST_PASSWORD ?? 'password123';

    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', password);

    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('http://localhost:3000/');

    await expect(page.getByRole('button', { name: 'Sign In' })).not.toBeVisible();
  });

  test('메모, 코멘트 플로우(생성, 수정, 삭제)', async ({ page }) => {
    await page.goto('/canvas');

    await expect(page.locator('.react-flow')).toBeVisible();

    // 메모 생성
    await page.getByRole('button').filter({ hasText: /^$/ }).nth(4).click();
    await page.locator('.react-flow').click({ position: { x: 300, y: 300 } });

    await expect(page.getByPlaceholder('Add a Memo')).toBeVisible();

    // 메모 작성
    await page.getByPlaceholder('Add a Memo').fill('test memo');
    await page.locator('.w-8').click();

    await expect(page.locator('.memo-icon')).toBeVisible();

    // 2번째 코멘트 작성
    await page.locator('.memo-icon').click();
    await page.getByRole('textbox', { name: 'Reply' }).fill('test memo2');
    await page.locator('.w-8').click();

    await expect(page.getByText('test memo2')).toBeVisible();

    // 2번째 코멘트 삭제
    await page.locator('li:nth-child(2) > .flex-1 > .flex.items-center > .flex > .lucide.lucide-trash').click();

    await expect(page.getByText('test memo2')).not.toBeVisible();

    // 1번째 코멘트 수정
    await page.locator('.lucide.lucide-pencil').click();
    await page.getByRole('list').getByRole('textbox').fill('test memo3');
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText('test memo3')).toBeVisible();

    // 메모 삭제
    await page.getByRole('button', { name: 'Resolved Memo' }).first().click();

    await expect(page.locator('.memo-icon')).not.toBeVisible();
  });
});