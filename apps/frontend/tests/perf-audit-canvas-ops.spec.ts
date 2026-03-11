import { test, expect } from '@playwright/test';
import { writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const PERF_EMAIL = process.env.PERF_EMAIL || 'test@example.com';
const PERF_PASSWORD = process.env.PERF_PASSWORD || 'password123';
const CANVAS_PATH = process.env.PERF_PATH || '/canvas';

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

test('perf-canvas-ops: 캔버스 조작 성능 진단', async ({
  page,
  context,
  browserName,
}) => {
  test.skip(browserName !== 'chromium', 'CDP 성능 측정은 Chromium 전용');

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
  await expect(page.getByRole('button', { name: 'Sign In' })).not.toBeVisible();

  await page.goto(CANVAS_PATH);
  await expect(page).not.toHaveURL(/.*\/signin/, { timeout: 5000 });
  await expect(page.locator('.react-flow')).toBeVisible({ timeout: 15000 });

  await page.evaluate(() => {
    (window as any).__PERF_RENDERS__ = [];
    (window as any).__PERF_LONG_TASKS__ = [];
  });

  const reactFlowBox = await page.locator('.react-flow').boundingBox();
  if (!reactFlowBox) throw new Error('.react-flow boundingBox를 찾을 수 없음');

  const addTable1 = await measureOp(page, async () => {
    await page.getByTestId('toolbar-table').click();
    const pane = await page.locator('.react-flow__pane').boundingBox();
    if (pane) {
      await page.mouse.click(
        pane.x + pane.width * 0.3,
        pane.y + pane.height * 0.3,
      );
    }
    await page.waitForTimeout(500);
    await page.getByTestId('toolbar-pointer').click();
  });

  const addTable2 = await measureOp(page, async () => {
    await page.getByTestId('toolbar-table').click();
    const pane = await page.locator('.react-flow__pane').boundingBox();
    if (pane) {
      await page.mouse.click(
        pane.x + pane.width * 0.7,
        pane.y + pane.height * 0.7,
      );
    }
    await page.waitForTimeout(500);
    await page.getByTestId('toolbar-pointer').click();
  });

  const addColBtn = page.getByTitle('Add Column').first();
  const addColumn = (await addColBtn.isVisible())
    ? await measureOp(page, async () => {
        await addColBtn.click();
        await page.waitForTimeout(300);
      })
    : { renderCount: 0, longTaskCount: 0, durationMs: 0, skipped: true };

  const edgeHandles = page.locator('.react-flow__handle');
  const edgeHandleCount = await edgeHandles.count();
  const connectEdge =
    edgeHandleCount >= 8
      ? await measureOp(page, async () => {
          const sourceBox = await edgeHandles.nth(1).boundingBox();
          const targetBox = await edgeHandles.nth(4).boundingBox();
          if (sourceBox && targetBox) {
            const sx = sourceBox.x + sourceBox.width / 2;
            const sy = sourceBox.y + sourceBox.height / 2;
            const tx = targetBox.x + targetBox.width / 2;
            const ty = targetBox.y + targetBox.height / 2;
            await page.mouse.move(sx, sy);
            await page.mouse.down();
            await page.mouse.move(tx, ty, { steps: 15 });
            await page.mouse.up();
            await page.waitForTimeout(500);
          }
        })
      : { renderCount: 0, longTaskCount: 0, durationMs: 0, skipped: true };

  const { metrics } = await cdp.send('Performance.getMetrics');
  const heapUsage = await cdp.send('Runtime.getHeapUsage');

  const cdpMetrics = Object.fromEntries(
    metrics.map((m: { name: string; value: number }) => [m.name, m.value]),
  );

  const result = {
    path: CANVAS_PATH,
    timestamp: new Date().toISOString(),
    ops: {
      addTable1,
      addTable2,
      addColumn,
      connectEdge,
    },
    heap: {
      usedMB: Math.round((heapUsage.usedSize / 1024 / 1024) * 10) / 10,
      totalMB: Math.round((heapUsage.totalSize / 1024 / 1024) * 10) / 10,
    },
    cdpMetrics: {
      scriptDuration: cdpMetrics['ScriptDuration'],
      layoutDuration: cdpMetrics['LayoutDuration'],
      recalcStyleDuration: cdpMetrics['RecalcStyleDuration'],
      layoutCount: cdpMetrics['LayoutCount'],
      recalcStyleCount: cdpMetrics['RecalcStyleCount'],
      jsEventListeners: cdpMetrics['JSEventListeners'],
      nodes: cdpMetrics['Nodes'],
    },
  };

  const resultsDir = join(__dirname, '../performance/results');
  mkdirSync(resultsDir, { recursive: true });

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const resultFile = join(resultsDir, `${timestamp}_canvas-ops.json`);

  writeFileSync(resultFile, JSON.stringify(result, null, 2));
  console.log(`\n측정 완료: ${resultFile}`);
});
