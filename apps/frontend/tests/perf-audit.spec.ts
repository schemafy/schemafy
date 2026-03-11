import { test, expect } from '@playwright/test';
import { writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const PERF_PATH = process.env.PERF_PATH || '/';
const PERF_EMAIL = process.env.PERF_EMAIL || 'test@example.com';
const PERF_PASSWORD = process.env.PERF_PASSWORD || 'password123';

// RequireAuth 적용 경로
const AUTH_ROUTES = ['/canvas'];
const needsAuth = AUTH_ROUTES.some((route) => PERF_PATH.startsWith(route));

test('perf-audit: 로컬 성능 진단 (회귀 차단 아님)', async ({
  page,
  context,
  browserName,
}) => {
  test.skip(browserName !== 'chromium', 'CDP 성능 측정은 Chromium 전용');

  const cdp = await context.newCDPSession(page);
  await cdp.send('Performance.enable');
  await cdp.send('Runtime.enable');

  await page.addInitScript(() => {
    (window as any).__PERF_VITALS__ = { cls: 0 };
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

    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'largest-contentful-paint') {
          (window as any).__PERF_VITALS__.lcp = entry.startTime;
        }
        if (entry.name === 'first-contentful-paint') {
          (window as any).__PERF_VITALS__.fcp = entry.startTime;
        }
        if (
          entry.entryType === 'layout-shift' &&
          !(entry as any).hadRecentInput
        ) {
          (window as any).__PERF_VITALS__.cls += (entry as any).value;
        }
      }
    });
    try {
      observer.observe({
        entryTypes: ['largest-contentful-paint', 'paint', 'layout-shift'],
      });
    } catch {
      // 일부 브라우저 미지원 시 무시
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

  if (needsAuth) {
    await page.goto('/signin');
    await page.fill('input[name="email"]', PERF_EMAIL);
    await page.fill('input[name="password"]', PERF_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');

    await expect(
      page.getByRole('button', { name: 'Sign In' }),
    ).not.toBeVisible();
  }

  await page.evaluate(() => {
    (window as any).__PERF_VITALS__ = { cls: 0 };
    (window as any).__PERF_RENDERS__ = [];
  });

  const navStart = Date.now();
  await page.goto(PERF_PATH);

  if (PERF_PATH.startsWith('/canvas')) {
    await expect(page).not.toHaveURL(/.*\/signin/, { timeout: 5000 });
    await expect(page.locator('.react-flow')).toBeVisible({ timeout: 15000 });
  } else {
    await page.waitForLoadState('load');
  }

  const loadTime = Date.now() - navStart;

  await Promise.race([
    page
      .waitForFunction(() => (window as any).__PERF_VITALS__?.lcp != null)
      .catch(() => {}),
    page.waitForTimeout(3000),
  ]);

  // 상호작용 측정: 마우스 이동 · 휠 줌 · 패닝 (캔버스 경로 전용)
  let interaction: {
    durationMs: number;
    phases: {
      mouseMove: { renderCount: number; longTaskCount: number };
      wheelZoom: { renderCount: number; longTaskCount: number };
      pan: { renderCount: number; longTaskCount: number };
    };
    totalRenderCount: number;
    rendersPerSec: number;
    totalLongTaskCount: number;
  } | null = null;

  if (PERF_PATH.startsWith('/canvas')) {
    const reactFlowEl = page.locator('.react-flow');
    const box = await reactFlowEl.boundingBox();

    if (box) {
      const cx = box.x + box.width / 2;
      const cy = box.y + box.height / 2;

      const measurePhase = async (action: () => Promise<void>) => {
        const rendersBefore = await page.evaluate(
          () => ((window as any).__PERF_RENDERS__ || []).length,
        );
        const longTasksBefore = await page.evaluate(
          () => ((window as any).__PERF_LONG_TASKS__ || []).length,
        );
        await action();
        const rendersAfter = await page.evaluate(
          () => ((window as any).__PERF_RENDERS__ || []).length,
        );
        const longTasksAfter = await page.evaluate(
          () => ((window as any).__PERF_LONG_TASKS__ || []).length,
        );
        return {
          renderCount: rendersAfter - rendersBefore,
          longTaskCount: longTasksAfter - longTasksBefore,
        };
      };

      const interactionStart = Date.now();

      // Phase 1: 수평 마우스 이동
      const mouseMovePh = await measurePhase(async () => {
        const steps = 20;
        for (let i = 0; i <= steps; i++) {
          await page.mouse.move(box.x + (box.width / steps) * i, cy);
          await page.waitForTimeout(150);
        }
      });

      // Phase 2: 휠 줌 인/아웃
      const wheelZoomPh = await measurePhase(async () => {
        await page.mouse.move(cx, cy);
        for (let i = 0; i < 5; i++) {
          await page.mouse.wheel(0, -100);
          await page.waitForTimeout(80);
        }
        for (let i = 0; i < 5; i++) {
          await page.mouse.wheel(0, 100);
          await page.waitForTimeout(80);
        }
      });

      // Phase 3: 패닝 (캔버스 드래그)
      const panPh = await measurePhase(async () => {
        await page.mouse.move(cx, cy);
        await page.mouse.down();
        await page.mouse.move(cx - 200, cy, { steps: 10 });
        await page.mouse.move(cx, cy, { steps: 10 });
        await page.mouse.up();
        await page.waitForTimeout(300);
      });

      const elapsed = Date.now() - interactionStart;
      const totalRenderCount =
        mouseMovePh.renderCount + wheelZoomPh.renderCount + panPh.renderCount;

      interaction = {
        durationMs: elapsed,
        phases: { mouseMove: mouseMovePh, wheelZoom: wheelZoomPh, pan: panPh },
        totalRenderCount,
        rendersPerSec: Math.round((totalRenderCount / elapsed) * 1000 * 10) / 10,
        totalLongTaskCount:
          mouseMovePh.longTaskCount +
          wheelZoomPh.longTaskCount +
          panPh.longTaskCount,
      };
    }
  }

  const { metrics } = await cdp.send('Performance.getMetrics');
  const heapUsage = await cdp.send('Runtime.getHeapUsage');

  const navTiming = await page.evaluate(() => {
    const nav = performance.getEntriesByType(
      'navigation',
    )[0] as PerformanceNavigationTiming;
    if (!nav) return null;
    return {
      ttfb: Math.round(nav.responseStart - nav.startTime),
      domInteractive: Math.round(nav.domInteractive),
      domComplete: Math.round(nav.domComplete),
      loadEventEnd: Math.round(nav.loadEventEnd),
    };
  });

  const vitals = await page.evaluate(() => {
    const v = (window as any).__PERF_VITALS__ || {};
    return {
      lcp: v.lcp ? Math.round(v.lcp) : null,
      fcp: v.fcp ? Math.round(v.fcp) : null,
      cls: v.cls != null ? Math.round(v.cls * 1000) / 1000 : null,
    };
  });

  const renderCount = await page.evaluate(
    () => ((window as any).__PERF_RENDERS__ || []).length,
  );

  const cdpMetrics = Object.fromEntries(
    metrics.map((m: { name: string; value: number }) => [m.name, m.value]),
  );

  const resultsDir = join(__dirname, '../performance/results');
  mkdirSync(resultsDir, { recursive: true });

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const safePath =
    PERF_PATH.replace(/[^a-zA-Z0-9-]/g, '_').replace(/^_+|_+$/g, '') || 'root';
  const resultFile = join(resultsDir, `${timestamp}_${safePath}.json`);

  const result = {
    path: PERF_PATH,
    timestamp: new Date().toISOString(),
    loadTime,
    vitals,
    navTiming,
    renderCount,
    interaction,
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
      documents: cdpMetrics['Documents'],
    },
  };

  writeFileSync(resultFile, JSON.stringify(result, null, 2));
  console.log(`\n측정 완료: ${resultFile}`);
});
