import { test } from '@playwright/test';
import { writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const PERF_EMAIL = process.env.PERF_EMAIL || 'test@example.com';
const PERF_PASSWORD = process.env.PERF_PASSWORD || 'password123';
const CANVAS_PATH = process.env.PERF_PATH || '/canvas';

function gridPosition(i: number, total: number): [number, number] {
  const cols = Math.ceil(Math.sqrt(total));
  const rows = Math.ceil(total / cols);
  const col = i % cols;
  const row = Math.floor(i / cols);
  const xRatio = cols > 1 ? 0.1 + (col / (cols - 1)) * 0.8 : 0.5;
  const yRatio = rows > 1 ? 0.15 + (row / (rows - 1)) * 0.65 : 0.4;
  return [xRatio, yRatio];
}

async function measureOp(
  page: import('@playwright/test').Page,
  action: () => Promise<void>,
): Promise<{ renderCount: number; longTaskCount: number; durationMs: number }> {
  const rendersBefore = await page.evaluate(
    () => ((window as any).__PERF_RENDERS__ || []).length,
  );
  const longTasksBefore = await page.evaluate(
    () => ((window as any).__PERF_LONG_TASKS__ || []).length,
  );
  const start = Date.now();
  await action();
  const durationMs = Date.now() - start;
  const rendersAfter = await page.evaluate(
    () => ((window as any).__PERF_RENDERS__ || []).length,
  );
  const longTasksAfter = await page.evaluate(
    () => ((window as any).__PERF_LONG_TASKS__ || []).length,
  );
  return {
    renderCount: rendersAfter - rendersBefore,
    longTaskCount: longTasksAfter - longTasksBefore,
    durationMs,
  };
}

async function addTableQuick(
  page: import('@playwright/test').Page,
  xRatio: number,
  yRatio: number,
): Promise<void> {
  await page.getByTestId('toolbar-table').click();
  const pane = await page.locator('.react-flow__pane').boundingBox();
  if (pane) {
    await page.mouse.click(
      pane.x + pane.width * xRatio,
      pane.y + pane.height * yRatio,
    );
  }
  await page.waitForTimeout(250);
  await page.getByTestId('toolbar-pointer').click();
  await page.waitForTimeout(100);
}

async function tryConnectEdge(
  page: import('@playwright/test').Page,
): Promise<void> {
  const handles = page.locator('.react-flow__handle');
  const handleCount = await handles.count();
  if (handleCount >= 8) {
    const sourceBox = await handles.nth(1).boundingBox();
    const targetBox = await handles.nth(4).boundingBox();
    if (sourceBox && targetBox) {
      const sx = sourceBox.x + sourceBox.width / 2;
      const sy = sourceBox.y + sourceBox.height / 2;
      const tx = targetBox.x + targetBox.width / 2;
      const ty = targetBox.y + targetBox.height / 2;
      await page.mouse.move(sx, sy);
      await page.mouse.down();
      await page.mouse.move(tx, ty, { steps: 15 });
      await page.mouse.up();
      await page.waitForTimeout(400);
    }
  }
}

async function resetCounters(
  page: import('@playwright/test').Page,
): Promise<void> {
  await page.evaluate(() => {
    (window as any).__PERF_RENDERS__ = [];
    (window as any).__PERF_LONG_TASKS__ = [];
  });
}

test('perf-canvas-scale: 규모별 캔버스 조작 성능 진단', async ({
  page,
  context,
  browserName,
}) => {
  test.skip(browserName !== 'chromium', 'CDP 성능 측정은 Chromium 전용');
  test.setTimeout(300_000);

  const cdp = await context.newCDPSession(page);
  await cdp.send('Performance.enable');
  await cdp.send('Runtime.enable');

  await page.addInitScript(() => {
    (window as any).__PERF_RENDERS__ = [];
    (window as any).__PERF_LONG_TASKS__ = [];

    try {
      const ltObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          (window as any).__PERF_LONG_TASKS__.push({
            startTime: Math.round(entry.startTime),
            duration: Math.round(entry.duration),
          });
        }
      });
      ltObserver.observe({ entryTypes: ['longtask'] });
    } catch {
      // longtask 미지원 브라우저 무시
    }

    Object.defineProperty(window, '__REACT_DEVTOOLS_GLOBAL_HOOK__', {
      configurable: true,
      enumerable: true,
      get: function () {
        return undefined;
      },
      set: function (hook) {
        const origCommit = hook.onCommitFiberRoot;
        hook.onCommitFiberRoot = function (...args: any[]) {
          (window as any).__PERF_RENDERS__.push({
            timestamp: performance.now(),
          });
          if (origCommit) origCommit.apply(this, args);
        };
        Object.defineProperty(window, '__REACT_DEVTOOLS_GLOBAL_HOOK__', {
          value: hook,
          writable: true,
          configurable: true,
          enumerable: true,
        });
      },
    });
  });

  await page.goto('/signin');
  await page.fill('input[name="email"]', PERF_EMAIL);
  await page.fill('input[name="password"]', PERF_PASSWORD);
  await page.click('button[type="submit"]');
  await page.waitForURL('**/');

  await page.goto(CANVAS_PATH);
  await page.waitForURL((url) => !url.pathname.includes('/signin'), {
    timeout: 5000,
  });
  await page
    .locator('.react-flow')
    .waitFor({ state: 'visible', timeout: 15000 });

  await resetCounters(page);

  const SCALE_LEVELS = [3, 5, 10];
  const MAX_TABLES = SCALE_LEVELS[SCALE_LEVELS.length - 1];
  const allPositions = Array.from({ length: MAX_TABLES + 1 }, (_, i) =>
    gridPosition(i, MAX_TABLES + 1),
  );

  let currentTableCount = 0;
  const scaleResults: Array<{
    tableCount: number;
    ops: { addTable: object; addColumn: object; connectEdge: object };
    heap: { usedMB: number; totalMB: number };
    cdpMetrics: object;
  }> = [];

  for (const targetCount of SCALE_LEVELS) {
    for (let i = currentTableCount; i < targetCount; i++) {
      const [x, y] = allPositions[i];
      await addTableQuick(page, x, y);
    }
    currentTableCount = targetCount;

    await resetCounters(page);

    const [addTableX, addTableY] = allPositions[currentTableCount];
    const addTable = await measureOp(page, async () => {
      await addTableQuick(page, addTableX, addTableY);
    });
    currentTableCount++;

    const addColBtn = page.getByTitle('Add Column').first();
    const addColumn = (await addColBtn.isVisible())
      ? await measureOp(page, async () => {
          await addColBtn.click({ force: true });
          await page.waitForTimeout(300);
        })
      : { renderCount: 0, longTaskCount: 0, durationMs: 0, skipped: true };

    const edgeHandleCount = await page.locator('.react-flow__handle').count();
    const connectEdge = edgeHandleCount >= 8
      ? await measureOp(page, async () => { await tryConnectEdge(page); })
      : { renderCount: 0, longTaskCount: 0, durationMs: 0, skipped: true };

    const { metrics } = await cdp.send('Performance.getMetrics');
    const heapUsage = await cdp.send('Runtime.getHeapUsage');
    const cdpMap = Object.fromEntries(
      metrics.map((m: { name: string; value: number }) => [m.name, m.value]),
    );

    scaleResults.push({
      tableCount: currentTableCount,
      ops: { addTable, addColumn, connectEdge },
      heap: {
        usedMB: Math.round((heapUsage.usedSize / 1024 / 1024) * 10) / 10,
        totalMB: Math.round((heapUsage.totalSize / 1024 / 1024) * 10) / 10,
      },
      cdpMetrics: {
        jsEventListeners: cdpMap['JSEventListeners'],
        nodes: cdpMap['Nodes'],
        layoutCount: cdpMap['LayoutCount'],
        recalcStyleCount: cdpMap['RecalcStyleCount'],
        scriptDuration: cdpMap['ScriptDuration'],
      },
    });
  }

  await resetCounters(page);
  const repeatColumnRuns: Array<{
    run: number;
    renderCount: number;
    longTaskCount: number;
    durationMs: number;
  }> = [];

  for (let i = 0; i < 5; i++) {
    const repeatColBtn = page.getByTitle('Add Column').first();
    const result = (await repeatColBtn.isVisible())
      ? await measureOp(page, async () => {
          await repeatColBtn.click({ force: true });
          await page.waitForTimeout(300);
        })
      : { renderCount: 0, longTaskCount: 0, durationMs: 0, skipped: true };
    repeatColumnRuns.push({ run: i + 1, ...result });
  }

  const result = {
    path: CANVAS_PATH,
    timestamp: new Date().toISOString(),
    scaleLevels: SCALE_LEVELS,
    scaleResults,
    repeatColumn: {
      tableCount: currentTableCount,
      runs: repeatColumnRuns,
    },
  };

  const resultsDir = join(__dirname, '../performance/results');
  mkdirSync(resultsDir, { recursive: true });

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const resultFile = join(resultsDir, `${timestamp}_canvas-scale.json`);

  writeFileSync(resultFile, JSON.stringify(result, null, 2));
  console.log(`\n측정 완료: ${resultFile}`);
});
