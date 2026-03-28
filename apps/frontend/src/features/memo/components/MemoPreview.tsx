import { useEffect, useRef } from 'react';
import { useStore } from '@xyflow/react';
import { useMouseTrackingPreview } from '@/features/drawing/hooks/useMouseTrackingPreview';

export const MemoPreview = () => {
  const zoom = useStore((s) => s.transform[2]);
  const zoomRef = useRef(zoom);

  useEffect(() => {
    zoomRef.current = zoom;
  }, [zoom]);

  const divRef = useMouseTrackingPreview(
    (position) =>
      `translate3d(${position.x}px, ${position.y - 48}px, 0) scale(${zoomRef.current})`,
  );

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
