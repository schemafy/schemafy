import { useRef, useEffect } from 'react';
import { useStore } from '@xyflow/react';

export const TablePreview = () => {
  const divRef = useRef<HTMLDivElement>(null);
  const zoom = useStore((s) => s.transform[2]);
  const zoomRef = useRef(zoom);
  const frameRef = useRef<number | null>(null);
  const latestPositionRef = useRef<{ x: number; y: number } | null>(null);

  useEffect(() => {
    zoomRef.current = zoom;
    const el = divRef.current;
    if (!el) return;
    el.style.width = `${200 * zoom}px`;
    el.style.height = `${100 * zoom}px`;
  }, [zoom]);

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
        el.style.transform = `translate3d(${position.x}px, ${
          position.y - 60
        }px, 0)`;
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

  return (
    <div
      ref={divRef}
      className="bg-schemafy-button-bg rounded-lg"
      style={{
        display: 'none',
        position: 'absolute',
        pointerEvents: 'none',
        zIndex: 999,
        opacity: 0.4,
        width: `${200 * zoom}px`,
        height: `${100 * zoom}px`,
        willChange: 'transform',
      }}
    />
  );
};
