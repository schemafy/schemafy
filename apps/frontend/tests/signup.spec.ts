import { expect, test } from '@playwright/test';

const publicApi = '**/public/api/v1.0';
const smtpEnabled = (process.env.SMTP_ENABLED ?? 'false') !== 'false';

test.describe('Sign-up Flow', () => {
  test('인증 메일이 비활성화되면 인증 UI 없이 가입한다', async ({ page }) => {
    test.skip(smtpEnabled, 'SMTP_ENABLED=false 환경에서 실행한다.');

    let signUpBody: Record<string, unknown> | undefined;

    await page.route(`${publicApi}/users/signup`, async (route) => {
      signUpBody = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: {
          Authorization: 'Bearer test-access-token',
          'Access-Control-Expose-Headers': 'Authorization',
        },
        body: JSON.stringify({
          id: 'user-1',
          email: 'test@example.com',
          name: 'Tester',
        }),
      });
    });

    await page.goto('/signup');

    await expect(page.getByLabel('Verification Code *')).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Send' })).not.toBeVisible();

    await page.getByLabel('Email *').fill('test@example.com');
    await page.getByLabel('Name *').fill('Tester');
    await page.getByLabel('Password *', { exact: true }).fill('password');
    await page.getByLabel('Confirm Password *').fill('password');
    await page.getByRole('button', { name: 'Create Account' }).click();

    await expect(page).toHaveURL('http://localhost:3001/');
    expect(signUpBody).toEqual({
      email: 'test@example.com',
      name: 'Tester',
      password: 'password',
    });
  });

  test('인증 메일이 활성화되면 인증 UI와 토큰을 요구한다', async ({ page }) => {
    test.skip(!smtpEnabled, 'SMTP_ENABLED=true 환경에서 실행한다.');

    let signUpRequested = false;

    await page.route(`${publicApi}/users/signup`, async (route) => {
      signUpRequested = true;
      await route.abort();
    });

    await page.goto('/signup');

    await expect(page.getByLabel('Verification Code *')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Send' })).toBeVisible();

    await page.getByLabel('Email *').fill('test@example.com');
    await page.getByLabel('Name *').fill('Tester');
    await page.getByLabel('Password *', { exact: true }).fill('password');
    await page.getByLabel('Confirm Password *').fill('password');
    await page.getByRole('button', { name: 'Create Account' }).click();

    await expect(page.getByRole('alert')).toContainText(
      'Please verify your email to continue.',
    );
    expect(signUpRequested).toBe(false);
  });

  test('서버에서 인증 메일이 비활성화되면 전용 오류 메시지를 표시한다', async ({
    page,
  }) => {
    test.skip(!smtpEnabled, 'SMTP_ENABLED=true 환경에서 실행한다.');

    await page.route(`${publicApi}/users/signup/email-code`, async (route) => {
      await route.fulfill({
        status: 409,
        contentType: 'application/problem+json',
        body: JSON.stringify({
          status: 409,
          reason: 'USER_AUTH_MAIL_DISABLED',
        }),
      });
    });

    await page.goto('/signup');
    await page.getByLabel('Email *').fill('test@example.com');
    await page.getByRole('button', { name: 'Send' }).click();

    await expect(
      page.getByText('Email verification is currently disabled.'),
    ).toBeVisible();
  });

  test('비밀번호 확인이 다르면 Enter 제출을 차단한다', async ({ page }) => {
    let signUpRequested = false;

    await page.route(`${publicApi}/users/signup`, async (route) => {
      signUpRequested = true;
      await route.abort();
    });

    await page.goto('/signup');
    await page.getByLabel('Email *').fill('test@example.com');
    await page.getByLabel('Name *').fill('Tester');
    await page.getByLabel('Password *', { exact: true }).fill('password');
    await page.getByLabel('Confirm Password *').fill('different');
    await page.getByLabel('Confirm Password *').press('Enter');

    await expect(page.getByRole('alert').first()).toContainText(
      'Password does not match.',
    );
    expect(signUpRequested).toBe(false);
  });
});
