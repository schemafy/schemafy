import { useStore } from '@xyflow/react';

interface TablePreviewProps {
  mousePosition: { x: number; y: number } | null;
}

export const TablePreview = ({ mousePosition }: TablePreviewProps) => {
  const zoom = useStore((state) => state.transform[2]);

  return (
    mousePosition && (
      <div
        className="bg-schemafy-button-bg rounded-lg"
        style={{
          position: 'absolute',
          pointerEvents: 'none',
          zIndex: 999,
          opacity: 0.4,
          width: `${200 * zoom}px`,
          height: `${100 * zoom}px`,
          transform: `translate(${mousePosition.x}px, ${mousePosition.y - 60}px)`,
        }}
      />
    )
  );
};
