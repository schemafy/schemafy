import { expect, test, type Page, type Route } from '@playwright/test';
import { readFile } from 'node:fs/promises';

const PROJECT_ID = 'project-export-test';
const SCHEMA_ID = 'schema-export-test';
const SCHEMA_NAME = 'orders:/2026*?';
const DDL_CODE = 'CREATE TABLE `orders` (\n  `id` BIGINT NOT NULL\n);';
const MERMAID_CODE = 'erDiagram\n  ORDERS {\n    BIGINT id PK\n  }';

const corsHeaders = {
  'access-control-allow-origin': 'http://localhost:3000',
  'access-control-allow-credentials': 'true',
  'access-control-allow-headers': 'authorization, content-type',
  'access-control-allow-methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
  'access-control-expose-headers': 'authorization',
};

const fulfillJson = async (
  route: Route,
  body: unknown,
  status = 200,
  headers: Record<string, string> = {},
) => {
  await route.fulfill({
    status,
    contentType: 'application/json',
    headers: { ...corsHeaders, ...headers },
    body: JSON.stringify(body),
  });
};

type ExportHandler = (route: Route) => Promise<void> | void;

interface CanvasMockOptions {
  schemaName?: string;
  handleSnapshot?: (route: Route) => Promise<void>;
}

const openCanvas = async (
  page: Page,
  handleExport: ExportHandler,
  { schemaName = SCHEMA_NAME, handleSnapshot }: CanvasMockOptions = {},
) => {
  await page.routeWebSocket('**/ws/collaboration**', () => {});

  await page.route('**/api/v1.0/**', async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (request.method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    if (pathname.endsWith('/users/refresh')) {
      await fulfillJson(route, {}, 200, {
        authorization: 'Bearer export-test-token',
      });
      return;
    }

    if (pathname.endsWith('/users')) {
      await fulfillJson(route, {
        id: 'viewer-user',
        email: 'viewer@example.com',
        name: 'Export Viewer',
      });
      return;
    }

    if (pathname.endsWith(`/projects/${PROJECT_ID}`)) {
      await fulfillJson(route, {
        id: PROJECT_ID,
        workspaceId: 'workspace-export-test',
        dbVendorId: 1,
        name: 'Export Project',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
        currentUserRole: 'VIEWER',
      });
      return;
    }

    if (pathname.endsWith(`/projects/${PROJECT_ID}/schemas`)) {
      await fulfillJson(route, [
        {
          id: SCHEMA_ID,
          projectId: PROJECT_ID,
          name: schemaName,
          charset: 'utf8mb4',
          collation: 'utf8mb4_0900_ai_ci',
          currentRevision: 73,
        },
      ]);
      return;
    }

    if (pathname.endsWith(`/schemas/${SCHEMA_ID}/snapshots`)) {
      if (handleSnapshot) return handleSnapshot(route);
      await fulfillJson(route, { currentRevision: 73, snapshots: {} });
      return;
    }

    if (pathname.endsWith(`/schemas/${SCHEMA_ID}/memos-with-comments`)) {
      await fulfillJson(route, []);
      return;
    }

    if (pathname.includes(`/schemas/${SCHEMA_ID}/exports/`)) {
      await handleExport(route);
      return;
    }

    await route.abort('failed');
  });

  await page.goto(`/project/${PROJECT_ID}`);
  await expect(
    page.getByRole('button', { name: 'Export', exact: true }),
  ).toBeVisible();
};

const openExportMenu = async (page: Page) => {
  await page.getByRole('button', { name: 'Export', exact: true }).click();
  return page.getByRole('menuitem', { name: 'Export', exact: true });
};

test('DDL과 Mermaid를 요청하고 원문을 복사·다운로드한다', async ({ page }) => {
  const requestedUrls: string[] = [];
  let releaseDdl!: () => void;
  const ddlGate = new Promise<void>((resolve) => {
    releaseDdl = resolve;
  });

  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: async (text: string) => {
          (window as Window & { __copiedExport?: string }).__copiedExport =
            text;
        },
      },
    });
  });

  await openCanvas(page, async (route) => {
    const url = route.request().url();
    requestedUrls.push(url);

    if (new URL(url).pathname.endsWith('/exports/ddl')) {
      await ddlGate;
      await fulfillJson(route, {
        schemaId: SCHEMA_ID,
        currentRevision: 73,
        targetDbVendor: 'mysql',
        ddl: DDL_CODE,
      });
      return;
    }

    await fulfillJson(route, {
      schemaId: SCHEMA_ID,
      currentRevision: 73,
      mermaid: MERMAID_CODE,
    });
  });

  let exportAction = await openExportMenu(page);
  await expect(
    page.getByRole('menuitemradio', { name: 'MySQL DDL' }),
  ).toBeChecked();
  await expect(
    page.getByRole('menuitemradio', { name: 'Mermaid' }),
  ).toBeVisible();

  await exportAction.click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText('Generating export...');
  await expect.poll(() => requestedUrls.length).toBe(1);
  expect(new URL(requestedUrls[0]).pathname).toBe(
    `/api/v1.0/schemas/${SCHEMA_ID}/exports/ddl`,
  );
  expect(new URL(requestedUrls[0]).searchParams.get('targetDbVendor')).toBe(
    'mysql',
  );

  releaseDdl();
  await expect(dialog).toContainText(DDL_CODE);
  expect(requestedUrls).toHaveLength(1);
  await expect(dialog).toContainText(SCHEMA_NAME);
  await expect(dialog).not.toContainText('73');

  await dialog.getByRole('button', { name: 'Copy' }).click();
  await expect(dialog.getByRole('button', { name: 'Copied' })).toBeVisible();
  await expect
    .poll(() =>
      page.evaluate(
        () => (window as Window & { __copiedExport?: string }).__copiedExport,
      ),
    )
    .toBe(DDL_CODE);

  const ddlDownloadPromise = page.waitForEvent('download');
  await dialog.getByRole('button', { name: 'Download' }).click();
  const ddlDownload = await ddlDownloadPromise;
  expect(ddlDownload.suggestedFilename()).toBe('orders--2026--.sql');
  expect(await readFile(await ddlDownload.path(), 'utf8')).toBe(DDL_CODE);
  await expect(dialog).toBeVisible();

  await dialog.getByRole('button', { name: 'Close' }).click();
  exportAction = await openExportMenu(page);
  await page.getByRole('menuitemradio', { name: 'Mermaid' }).click();
  await exportAction.click();

  await expect(dialog).toContainText(MERMAID_CODE);
  await expect.poll(() => requestedUrls.length).toBe(2);
  expect(new URL(requestedUrls[1]).pathname).toBe(
    `/api/v1.0/schemas/${SCHEMA_ID}/exports/mermaid`,
  );
  expect(new URL(requestedUrls[1]).search).toBe('');

  const mermaidDownloadPromise = page.waitForEvent('download');
  await dialog.getByRole('button', { name: 'Download' }).click();
  const mermaidDownload = await mermaidDownloadPromise;
  expect(mermaidDownload.suggestedFilename()).toBe('orders--2026--.mmd');
  expect(await readFile(await mermaidDownload.path(), 'utf8')).toBe(
    MERMAID_CODE,
  );
});

test('스냅샷 오류를 재시도할 때도 헤더를 유지한다', async ({ page }) => {
  let failing = true;
  await openCanvas(page, (route) => fulfillJson(route, {}), {
    handleSnapshot: (route) =>
      failing
        ? fulfillJson(
            route,
            { code: 'INTERNAL_SERVER_ERROR', category: 'USER_FEEDBACK' },
            500,
          )
        : fulfillJson(route, { currentRevision: 73, snapshots: {} }),
  });
  await page
    .getByRole('banner')
    .evaluate((element) =>
      element.setAttribute('data-test-persistent', 'true'),
    );
  await expect(page.getByRole('main')).toContainText(
    'Failed to load schema data.',
    { timeout: 15000 },
  );
  await expect(
    page.locator('header[data-test-persistent="true"]'),
  ).toBeVisible();
  failing = false;
  await page.getByRole('button', { name: 'Retry' }).click();
  await expect(
    page.getByRole('button', { name: 'Expand schemas' }),
  ).toBeVisible();
  await expect(
    page.locator('header[data-test-persistent="true"]'),
  ).toBeVisible();
});

for (const failure of ['project', 'schema'] as const) {
  test(`${failure} 로딩과 오류 화면에도 캔버스 레이아웃을 유지한다`, async ({
    page,
  }) => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    let reached!: () => void;
    const requestReached = new Promise<void>((resolve) => {
      reached = resolve;
    });
    await page.route('**/api/v1.0/**', async (route) => {
      const req = route.request();
      const path = new URL(req.url()).pathname;
      if (req.method() === 'OPTIONS')
        return route.fulfill({ status: 204, headers: corsHeaders });
      if (path.endsWith('/users/refresh'))
        return fulfillJson(route, {}, 200, { authorization: 'Bearer test' });
      if (path.endsWith('/users'))
        return fulfillJson(route, {
          id: 'u',
          name: 'User',
          email: 'u@example.com',
        });
      if (path.endsWith(`/projects/${PROJECT_ID}`)) {
        if (failure === 'project') {
          reached();
          await gate;
          return fulfillJson(
            route,
            { code: 'NOT_FOUND', category: 'USER_FEEDBACK' },
            404,
          );
        }
        return fulfillJson(route, {
          id: PROJECT_ID,
          workspaceId: 'w',
          dbVendorId: 1,
          name: 'Project',
          currentUserRole: 'VIEWER',
        });
      }
      if (path.endsWith(`/projects/${PROJECT_ID}/schemas`)) {
        reached();
        await gate;
        return fulfillJson(
          route,
          { code: 'INTERNAL_SERVER_ERROR', category: 'USER_FEEDBACK' },
          500,
        );
      }
      return route.abort();
    });
    await page.goto(`/project/${PROJECT_ID}`);
    await requestReached;
    await expect(page.getByRole('banner')).toBeVisible();
    await page
      .getByRole('banner')
      .evaluate((element) =>
        element.setAttribute('data-test-persistent', 'true'),
      );
    await expect(page.getByRole('main').getByRole('status')).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Export', exact: true }),
    ).toHaveCount(0);
    release();
    await expect(page.getByRole('main')).toContainText(
      failure === 'project' ? 'Page not found' : 'Failed to load schema data.',
      { timeout: 15000 },
    );
    await expect(page.getByRole('banner')).toBeVisible();
    await expect(
      page.locator('header[data-test-persistent="true"]'),
    ).toBeVisible();
  });
}

test('프로젝트와 스키마 로딩 및 스키마 전환 중에도 같은 헤더를 유지한다', async ({
  page,
}) => {
  const schema2 = 'schema-second';
  let releaseProject!: () => void;
  let releaseSchemas!: () => void;
  let releaseSnapshot!: () => void;
  const projectGate = new Promise<void>((resolve) => {
    releaseProject = resolve;
  });
  const schemasGate = new Promise<void>((resolve) => {
    releaseSchemas = resolve;
  });
  const snapshotGate = new Promise<void>((resolve) => {
    releaseSnapshot = resolve;
  });
  await page.routeWebSocket('**/ws/collaboration**', () => {});
  await page.route('**/api/v1.0/**', async (route) => {
    const req = route.request();
    const path = new URL(req.url()).pathname;
    if (req.method() === 'OPTIONS')
      return route.fulfill({ status: 204, headers: corsHeaders });
    if (path.endsWith('/users/refresh'))
      return fulfillJson(route, {}, 200, { authorization: 'Bearer test' });
    if (path.endsWith('/users'))
      return fulfillJson(route, {
        id: 'u',
        name: 'User',
        email: 'u@example.com',
      });
    if (path.endsWith(`/projects/${PROJECT_ID}`)) {
      await projectGate;
      return fulfillJson(route, {
        id: PROJECT_ID,
        workspaceId: 'w',
        dbVendorId: 1,
        name: 'Project',
        currentUserRole: 'VIEWER',
      });
    }
    if (path.endsWith(`/projects/${PROJECT_ID}/schemas`)) {
      await schemasGate;
      return fulfillJson(route, [
        {
          id: SCHEMA_ID,
          projectId: PROJECT_ID,
          name: 'First',
          charset: 'utf8mb4',
          collation: 'utf8mb4_0900_ai_ci',
        },
        {
          id: schema2,
          projectId: PROJECT_ID,
          name: 'Second',
          charset: 'utf8mb4',
          collation: 'utf8mb4_0900_ai_ci',
        },
      ]);
    }
    if (path.endsWith(`/schemas/${schema2}/snapshots`)) {
      await snapshotGate;
      return fulfillJson(route, { currentRevision: 1, snapshots: {} });
    }
    if (path.endsWith('/snapshots'))
      return fulfillJson(route, { currentRevision: 1, snapshots: {} });
    if (path.endsWith('/memos-with-comments')) return fulfillJson(route, []);
    if (path.endsWith(`/schemas/${schema2}/exports/ddl`))
      return fulfillJson(route, {
        schemaId: schema2,
        currentRevision: 1,
        targetDbVendor: 'mysql',
        ddl: DDL_CODE,
      });
    return route.abort();
  });
  await page.goto(`/project/${PROJECT_ID}`);
  const header = page.getByRole('banner');
  await expect(page.getByRole('status')).toContainText('Loading project...');
  await header.evaluate((element) =>
    element.setAttribute('data-test-persistent', 'true'),
  );
  releaseProject();
  await expect(page.getByRole('status')).toContainText('Loading schema...');
  await expect(
    page.locator('header[data-test-persistent="true"]'),
  ).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Export', exact: true }),
  ).toHaveCount(0);
  releaseSchemas();
  await page.getByRole('button', { name: 'Expand schemas' }).click();
  await page.getByText('Second', { exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Loading schema...');
  await expect(
    page.locator('header[data-test-persistent="true"]'),
  ).toBeVisible();
  releaseSnapshot();
  await expect(
    page.getByRole('button', { name: 'Expand schemas' }),
  ).toContainText('Second');
  const action = await openExportMenu(page);
  await action.click();
  await expect(page.getByRole('dialog')).toContainText(DDL_CODE);
  await expect(page.getByRole('dialog')).toContainText('Second');
});

test('로딩 중 닫으면 요청과 모달 상태를 초기화하고 형식은 유지한다', async ({
  page,
}) => {
  await page.addInitScript(() => {
    const reportedErrors: string[] = [];
    Object.assign(window, { __reportedErrors: reportedErrors });
    window.reportError = (error) => reportedErrors.push(String(error));
  });
  const pendingRoutes: Route[] = [];
  let exportCount = 0;

  await openCanvas(page, async (route) => {
    exportCount += 1;
    if (exportCount === 1) {
      await fulfillJson(route, {
        schemaId: SCHEMA_ID,
        currentRevision: 73,
        mermaid: 'previous export result',
      });
      return;
    }

    pendingRoutes.push(route);
  });

  let exportAction = await openExportMenu(page);
  await page.getByRole('menuitemradio', { name: 'Mermaid' }).click();
  await exportAction.click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText('previous export result');

  await dialog.getByRole('button', { name: 'Close' }).click();
  exportAction = await openExportMenu(page);
  await expect(
    page.getByRole('menuitemradio', { name: 'Mermaid' }),
  ).toBeChecked();
  await exportAction.click();

  await expect(dialog).toContainText('Generating export...');
  await expect(dialog).not.toContainText('previous export result');
  await expect.poll(() => pendingRoutes.length).toBe(1);

  const abortedRequest = page.waitForEvent('requestfailed', {
    predicate: (request) =>
      new URL(request.url()).pathname.endsWith('/exports/mermaid'),
  });
  await page.keyboard.press('Escape');
  await expect(dialog).not.toBeVisible();
  expect((await abortedRequest).failure()?.errorText).toBeTruthy();

  exportAction = await openExportMenu(page);
  await expect(
    page.getByRole('menuitemradio', { name: 'Mermaid' }),
  ).toBeChecked();
  await exportAction.click();
  await expect(dialog).toContainText('Generating export...');
  await expect.poll(() => pendingRoutes.length).toBe(2);

  await fulfillJson(pendingRoutes[1], {
    schemaId: SCHEMA_ID,
    currentRevision: 74,
    mermaid: MERMAID_CODE,
  });
  await expect(dialog).toContainText(MERMAID_CODE);
  const reportedErrors = await page.evaluate(
    () =>
      (window as Window & { __reportedErrors?: string[] }).__reportedErrors ??
      [],
  );
  expect(reportedErrors.filter((error) => error.includes('canceled'))).toEqual(
    [],
  );
});

test('존재하지 않는 프로젝트 하위 경로도 404 레이아웃을 표시한다', async ({
  page,
}) => {
  await page.route('**/api/v1.0/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (route.request().method() === 'OPTIONS')
      return route.fulfill({ status: 204, headers: corsHeaders });
    if (path.endsWith('/users/refresh'))
      return fulfillJson(route, {}, 200, { authorization: 'Bearer test' });
    if (path.endsWith('/users'))
      return fulfillJson(route, {
        id: 'u',
        name: 'User',
        email: 'u@example.com',
      });
    return route.abort();
  });
  await page.goto(`/project/${PROJECT_ID}/unknown`);
  await expect(page.getByRole('banner')).toBeVisible();
  await expect(page.getByRole('main')).toContainText('Page not found');
});

test('키보드로 Mermaid를 선택하고 내보낸다', async ({ page }) => {
  let requestedUrl = '';
  await openCanvas(page, async (route) => {
    requestedUrl = route.request().url();
    await fulfillJson(route, {
      schemaId: SCHEMA_ID,
      currentRevision: 73,
      mermaid: MERMAID_CODE,
    });
  });
  await page.getByRole('button', { name: 'Export', exact: true }).focus();
  await page.keyboard.press('Enter');
  await expect(
    page.getByRole('menuitemradio', { name: 'MySQL DDL' }),
  ).toBeFocused();
  await page.keyboard.press('ArrowDown');
  await expect(
    page.getByRole('menuitemradio', { name: 'Mermaid' }),
  ).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(
    page.getByRole('menuitemradio', { name: 'Mermaid' }),
  ).toBeChecked();
  await page.keyboard.press('ArrowDown');
  await expect(
    page.getByRole('menuitem', { name: 'Export', exact: true }),
  ).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('dialog')).toContainText(MERMAID_CODE);
  expect(new URL(requestedUrl).pathname.endsWith('/exports/mermaid')).toBe(
    true,
  );
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toBeHidden();
  await expect(
    page.getByRole('button', { name: 'Export', exact: true }),
  ).toBeFocused();
});

test('빈 스키마 이름은 기본 파일명으로 다운로드한다', async ({ page }) => {
  await openCanvas(
    page,
    (route) =>
      fulfillJson(route, {
        schemaId: SCHEMA_ID,
        currentRevision: 73,
        targetDbVendor: 'mysql',
        ddl: DDL_CODE,
      }),
    { schemaName: '' },
  );

  const exportAction = await openExportMenu(page);
  await exportAction.click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText(DDL_CODE);
  const downloadPromise = page.waitForEvent('download');
  await dialog.getByRole('button', { name: 'Download' }).click();
  expect((await downloadPromise).suggestedFilename()).toBe('schema.sql');
});

test('공통 오류를 표시하고 같은 요청을 Retry한다', async ({ page }) => {
  const requestedUrls: string[] = [];
  const errors = [
    {
      code: 'DDL_INVALID_VALUE',
      message:
        'This schema cannot be exported. Check the table and column settings.',
    },
    {
      code: 'DDL_UNSUPPORTED_VENDOR',
      message: 'DDL export is not supported for this database.',
    },
    {
      code: 'DDL_TABLE_NOT_FOUND',
      message:
        'A table required for export could not be found. Refresh and try again.',
    },
    {
      code: 'DDL_COLUMN_NOT_FOUND',
      message:
        'A column required for export could not be found. Refresh and try again.',
    },
  ];

  await openCanvas(page, async (route) => {
    requestedUrls.push(route.request().url());
    const error = errors[requestedUrls.length - 1];

    if (error) {
      await fulfillJson(
        route,
        {
          code: error.code,
          category: 'USER_FEEDBACK',
        },
        400,
      );
      return;
    }

    await fulfillJson(route, {
      schemaId: SCHEMA_ID,
      currentRevision: 73,
      targetDbVendor: 'mysql',
      ddl: DDL_CODE,
    });
  });

  const dialog = page.getByRole('dialog');
  for (const [index, error] of errors.entries()) {
    const exportAction = await openExportMenu(page);
    await exportAction.click();
    await expect(dialog).toContainText(error.message);

    if (index < errors.length - 1) {
      await dialog.getByRole('button', { name: 'Close' }).click();
    }
  }

  await dialog.getByRole('button', { name: 'Retry' }).click();
  await expect(dialog).toContainText(DDL_CODE);

  expect(requestedUrls).toHaveLength(errors.length + 1);
  expect(requestedUrls.at(-1)).toBe(requestedUrls.at(-2));
});
