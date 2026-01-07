import { useRef, useCallback, useEffect } from 'react';
import { debounce } from 'lodash-es';
import type { DebouncedFunc } from 'lodash-es';

interface DebouncedFunction<T extends (...args: unknown[]) => unknown> {
  (...args: Parameters<T>): ReturnType<DebouncedFunc<T>> | undefined;
  cancel: () => void;
  flush: () => ReturnType<T> | undefined;
}

export function useDebounce<T extends (...args: unknown[]) => unknown>(
  callback: T,
  delay: number,
): DebouncedFunction<T> {
  const debouncedRef = useRef<DebouncedFunc<T> | null>(null);

  useEffect(() => {
    debouncedRef.current = debounce(callback, delay);

    return () => {
      debouncedRef.current?.cancel();
    };
  }, [callback, delay]);

  const debouncedFunction = useCallback((...args: Parameters<T>) => {
    return debouncedRef.current?.(...args);
  }, []) as DebouncedFunction<T>;

  debouncedFunction.cancel = useCallback(() => {
    debouncedRef.current?.cancel();
  }, []);

  debouncedFunction.flush = useCallback(() => {
    return debouncedRef.current?.flush();
  }, []);

  return debouncedFunction;
}
