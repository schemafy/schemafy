import { useRef, useEffect } from 'react';

export const useMouseTrackingPreview = (
  buildTransform: (position: { x: number; y: number }) => string,
) => {
  const divRef = useRef<HTMLDivElement>(null);
  const frameRef = useRef<number | null>(null);
  const latestPositionRef = useRef<{ x: number; y: number } | null>(null);
  const buildTransformRef = useRef(buildTransform);
  buildTransformRef.current = buildTransform;

  useEffect(() => {
    const el = divRef.current;
    if (!el) return;

    const handleMouseMove = (e: MouseEvent) => {
      latestPositionRef.current = { x: e.clientX, y: e.clientY };

      if (frameRef.current !== null) return;

      frameRef.current = window.requestAnimationFrame(() => {
        frameRef.current = null;
        const position = latestPositionRef.current;
        if (!position) return;

        el.style.display = 'block';
        el.style.transform = buildTransformRef.current(position);
      });
    };

    window.addEventListener('mousemove', handleMouseMove);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);

      if (frameRef.current !== null) {
        window.cancelAnimationFrame(frameRef.current);
        frameRef.current = null;
      }
    };
  }, []);

  return divRef;
};
