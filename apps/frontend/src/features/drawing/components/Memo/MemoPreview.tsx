import { useStore } from "@xyflow/react";
import type { Point } from "../../types";

interface MemoPreviewProps {
  mousePosition: Point | null;
}
export const MemoPreview = ({ mousePosition }: MemoPreviewProps) => {
  const zoom = useStore((state) => state.transform[2]);

  if (!mousePosition) return null;

  return (
    <div
      className="bg-schemafy-button-bg rounded-br-full rounded-t-full"
      style={{
        position: 'absolute',
        pointerEvents: 'none',
        zIndex: 999,
        opacity: 0.4,
        width: '24px',
        height: '24px',
        transform: `translate3d(${mousePosition.x}px, ${mousePosition.y - 48}px, 0) scale(${zoom})`,
        transformOrigin: 'top left',
        willChange: 'transform',
      }}
    />
  );
};