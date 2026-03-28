import { useEffect } from 'react';
import { useStore } from '@xyflow/react';
import { useMouseTrackingPreview } from '../hooks/useMouseTrackingPreview';

export const TablePreview = () => {
  const zoom = useStore((s) => s.transform[2]);

  const divRef = useMouseTrackingPreview(
    (position) =>
      `translate3d(${position.x}px, ${position.y - 60}px, 0)`,
  );

  useEffect(() => {
    const el = divRef.current;
    if (!el) return;
    el.style.width = `${200 * zoom}px`;
    el.style.height = `${100 * zoom}px`;
  }, [zoom, divRef]);

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
