import { expect, test } from '@playwright/test';

for (const status of ['loading', 'error']) {
  test(`capability ${status} 중에도 기존 인덱스 메타데이터를 표시한다`, async ({
    page,
  }) => {
    await page.goto('/tests/fixtures/index-row-view-mode.html');

    await expect(page.getByTestId(`index-${status}`)).toContainText(
      'idx_email (email DESC) USING FULLTEXT',
    );
  });
}
