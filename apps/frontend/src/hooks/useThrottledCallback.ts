import { useCallback, useRef } from 'react';

export const useThrottledCallback = <Args extends unknown[]>(
  fn: (...args: Args) => void,
  intervalMs: number,
) => {
  const lastTimeRef = useRef(0);
  const fnRef = useRef(fn);
  fnRef.current = fn;

  return useCallback(
    (...args: Args) => {
      const now = Date.now();
      if (now - lastTimeRef.current < intervalMs) return;
      lastTimeRef.current = now;
      fnRef.current(...args);
    },
    [intervalMs],
  );
};
