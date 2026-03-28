import { useRef, useEffect } from 'react';
import { useStore } from '@xyflow/react';

export const MemoPreview = () => {
  const divRef = useRef<HTMLDivElement>(null);
  const zoom = useStore((s) => s.transform[2]);
  const zoomRef = useRef(zoom);
  const frameRef = useRef<number | null>(null);
  const latestPositionRef = useRef<{ x: number; y: number } | null>(null);

  useEffect(() => {
    zoomRef.current = zoom;
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
          position.y - 48
        }px, 0) scale(${zoomRef.current})`;
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
      className="bg-schemafy-button-bg rounded-br-full rounded-t-full"
      style={{
        display: 'none',
        position: 'absolute',
        pointerEvents: 'none',
        zIndex: 999,
        opacity: 0.4,
        width: '24px',
        height: '24px',
        transformOrigin: 'top left',
        willChange: 'transform',
      }}
    />
  );
};
