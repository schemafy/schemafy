import { useEffect, useRef } from 'react';

export const useInactivityTimer = (onClose: () => void, delayMs: number) => {
  const onCloseRef = useRef(onClose);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  const reset = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => onCloseRef.current(), delayMs);
  };

  useEffect(() => {
    reset();
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  return reset;
};
