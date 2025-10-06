import { useState, useEffect } from 'react';
import {
  BaseEdge,
  EdgeLabelRenderer,
  useReactFlow,
  Position,
  type EdgeProps,
} from '@xyflow/react';
import { Move } from 'lucide-react';

export const CustomSmoothStepEdge = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  style = {},
  markerEnd,
  markerStart,
  label,
  labelStyle,
  data,
}: EdgeProps) => {
  const { setEdges, screenToFlowPosition } = useReactFlow();
  const [isDragging, setIsDragging] = useState(false);

  const isHorizontal =
    sourcePosition === Position.Left || sourcePosition === Position.Right;

  const { controlPointX, controlPointY, path, handlePosition } = (() => {
    const defaultControlPointX = (sourceX + targetX) / 2;
    const defaultControlPointY = (sourceY + targetY) / 2;

    const currentControlPointX: number =
      typeof data?.controlPointX === 'number'
        ? data.controlPointX
        : defaultControlPointX;
    const currentControlPointY: number =
      typeof data?.controlPointY === 'number'
        ? data.controlPointY
        : defaultControlPointY;

    let edgePath = '';
    if (isHorizontal) {
      edgePath = `M ${sourceX},${sourceY} L ${currentControlPointX},${sourceY} L ${currentControlPointX},${targetY} L ${targetX},${targetY}`;
    } else {
      edgePath = `M ${sourceX},${sourceY} L ${sourceX},${currentControlPointY} L ${targetX},${currentControlPointY} L ${targetX},${targetY}`;
    }

    const handleX = isHorizontal
      ? currentControlPointX
      : (sourceX + targetX) / 2;
    const handleY = isHorizontal
      ? (sourceY + targetY) / 2
      : currentControlPointY;

    return {
      controlPointX: currentControlPointX,
      controlPointY: currentControlPointY,
      path: edgePath,
      handlePosition: { x: handleX, y: handleY },
    };
  })();

  const handleMouseDown = (e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    setIsDragging(true);
  };

  useEffect(() => {
    if (!isDragging) return;

    const handleMouseMove = (e: MouseEvent) => {
      const flowPosition = screenToFlowPosition({
        x: e.clientX,
        y: e.clientY,
      });

      setEdges((edges) =>
        edges.map((edge) => {
          if (edge.id !== id) return edge;

          const newControlPointX = isHorizontal
            ? flowPosition.x
            : controlPointX;
          const newControlPointY = isHorizontal
            ? controlPointY
            : flowPosition.y;

          return {
            ...edge,
            data: {
              ...edge.data,
              controlPointX: newControlPointX,
              controlPointY: newControlPointY,
            },
          };
        }),
      );
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [
    isDragging,
    id,
    setEdges,
    screenToFlowPosition,
    isHorizontal,
    controlPointX,
    controlPointY,
  ]);

  return (
    <>
      <BaseEdge
        path={path}
        markerEnd={markerEnd}
        markerStart={markerStart}
        style={style}
      />

      {label && (
        <EdgeLabelRenderer>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${handlePosition.x}px, ${handlePosition.y}px)`,
              fontSize: labelStyle?.fontSize || 12,
              fontWeight: labelStyle?.fontWeight || 'bold',
              color: labelStyle?.color || 'var(--color-schemafy-dark-gray)',
              pointerEvents: 'none',
              background: 'white',
              padding: '2px 6px',
              borderRadius: '4px',
              marginTop: '-25px',
            }}
          >
            {label}
          </div>
        </EdgeLabelRenderer>
      )}

      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${handlePosition.x}px, ${handlePosition.y}px)`,
            pointerEvents: 'all',
            cursor: isDragging ? 'grabbing' : 'grab',
            zIndex: 1000,
          }}
          className="nodrag nopan"
          onMouseDown={handleMouseDown}
        >
          <div
            className={`
              bg-schemafy-bg border-2 rounded-full shadow-lg transition-all
              ${isDragging ? 'scale-125 border-blue-500' : 'border-schemafy-dark-gray hover:border-blue-400'}
            `}
            style={{
              width: 20,
              height: 20,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Move
              size={10}
              className={
                isDragging ? 'text-blue-500' : 'text-schemafy-dark-gray'
              }
            />
          </div>
        </div>
      </EdgeLabelRenderer>
    </>
  );
};
