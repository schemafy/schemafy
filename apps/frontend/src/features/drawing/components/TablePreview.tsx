import { useRef, useEffect } from 'react';
import { useStore } from '@xyflow/react';

export const TablePreview = () => {
  const divRef = useRef<HTMLDivElement>(null);
  const zoom = useStore((s) => s.transform[2]);
  const zoomRef = useRef(zoom);

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
      el.style.display = 'block';
      el.style.transform = `translate(${e.clientX}px, ${e.clientY - 60}px)`;
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
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
      }}
    />
  );
};
