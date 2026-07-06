import { useState, useCallback } from 'react';
import { reportUnexpectedError } from '@/lib';

export const useLocalStorage = <T>(
  key: string,
  initialValue: T,
): [T, (value: T | ((prev: T) => T)) => void] => {
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = localStorage.getItem(key);
      return item ? (JSON.parse(item) as T) : initialValue;
    } catch (error) {
      reportUnexpectedError(error, {
        context: `Failed to read localStorage key "${key}".`,
      });
      return initialValue;
    }
  });

  const setValue = useCallback(
    (value: T | ((prev: T) => T)) => {
      setStoredValue((prev) => {
        try {
          const valueToStore = value instanceof Function ? value(prev) : value;
          localStorage.setItem(key, JSON.stringify(valueToStore));
          return valueToStore;
        } catch (error) {
          reportUnexpectedError(error, {
            context: `Failed to write localStorage key "${key}".`,
          });
          return prev;
        }
      });
    },
    [key],
  );

  return [storedValue, setValue];
};
