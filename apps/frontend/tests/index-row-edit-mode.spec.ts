import { expect, test } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('/tests/fixtures/index-row-edit-mode.html');
});

test('capability loading 중에는 타입/정렬방향 selector가 비활성 상태로 남아있는다', async ({
  page,
}) => {
  const scope = page.getByTestId('edit-loading');
  const comboboxes = scope.locator('button[role="combobox"]');

  await expect(comboboxes).toHaveCount(2);
  await expect(comboboxes.nth(0)).toBeDisabled();
  await expect(comboboxes.nth(1)).toBeDisabled();
});

test('capability error 상태에서는 supportedTypes가 남아있어도 타입 selector를 비활성화한다', async ({
  page,
}) => {
  const scope = page.getByTestId('edit-error-with-stale-data');
  const typeSelect = scope.locator('button[role="combobox"]').first();

  await expect(typeSelect).toBeDisabled();
});

test('Add Index 버튼도 capability error 상태에서는 supportedTypes와 무관하게 비활성화된다', async ({
  page,
}) => {
  const addButton = page
    .getByTestId('add-index-error-with-stale-data')
    .getByTestId('add-index-button');

  await expect(addButton).toBeDisabled();
});

test('vendor가 sort direction을 지원하지 않는 타입이면 ready 상태에서 정렬방향 selector가 아예 없다', async ({
  page,
}) => {
  const scope = page.getByTestId('edit-ready-unsupported-sortdir');
  const comboboxes = scope.locator('button[role="combobox"]');

  // type select만 남고 sort-direction select는 렌더링되지 않아야 한다
  await expect(comboboxes).toHaveCount(1);
});
