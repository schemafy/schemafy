import { useRef, useEffect } from 'react';
import { useStore } from '@xyflow/react';

export const MemoPreview = () => {
  const divRef = useRef<HTMLDivElement>(null);
  const zoom = useStore((s) => s.transform[2]);
  const zoomRef = useRef(zoom);

  useEffect(() => {
    zoomRef.current = zoom;
  }, [zoom]);

  useEffect(() => {
    const el = divRef.current;
    if (!el) return;

    const handleMouseMove = (e: MouseEvent) => {
      el.style.display = 'block';
      el.style.transform = `translate3d(${e.clientX}px, ${e.clientY - 48}px, 0) scale(${zoomRef.current})`;
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
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
