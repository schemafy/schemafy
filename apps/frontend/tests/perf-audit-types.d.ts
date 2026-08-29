export interface PerformanceAuditWindow extends Window {
  __PERF_VITALS__?: PerformanceAuditVitals
  __PERF_RENDERS__?: PerformanceAuditRender[]
  __PERF_LONG_TASKS__?: PerformanceAuditLongTask[]
  __REACT_DEVTOOLS_GLOBAL_HOOK__?: ReactDevToolsGlobalHook
}

export interface PerformanceAuditVitals {
  cls: number
  fcp?: number
  lcp?: number
}

export interface PerformanceAuditRender {
  timestamp: number
}

export interface PerformanceAuditLongTask {
  startTime: number
  duration: number
}

export interface PerformanceAuditOperation {
  renderCount: number
  longTaskCount: number
  durationMs: number
  skipped?: boolean
}

export interface PerformanceAuditCdpMetrics {
  [metricName: string]: number | undefined
}

export interface ReactDevToolsGlobalHook {
  onCommitFiberRoot?: (this: unknown, ...args: unknown[]) => void
}

export interface LayoutShiftPerformanceEntry extends PerformanceEntry {
  hadRecentInput: boolean
  value: number
}
