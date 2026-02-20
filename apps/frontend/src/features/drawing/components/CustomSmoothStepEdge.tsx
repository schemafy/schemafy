import {
  BaseEdge,
  EdgeLabelRenderer,
  type EdgeProps,
  type Edge,
} from '@xyflow/react';
import { Move } from 'lucide-react';
import type { Point, EdgeData } from '../types';
import { useControlPointDrag } from '../hooks/useControlPointDrag';

export const CustomSmoothStepEdge = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  style = {},
  markerEnd,
  markerStart,
  label,
  labelStyle,
  data,
}: EdgeProps<Edge<EdgeData>>) => {
  const {
    path,
    handle1Position,
    handle2Position,
    labelPosition,
    draggingHandle,
    handleMouseDown,
  } = useControlPointDrag({
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    data,
  });

  const renderHandle = (position: Point, handleIndex: number) => {
    const isActive = draggingHandle === handleIndex;

    return (
      <EdgeLabelRenderer key={handleIndex}>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${position.x}px, ${position.y}px)`,
            pointerEvents: 'all',
            cursor: isActive ? 'grabbing' : 'grab',
            zIndex: 1000,
          }}
          className="nodrag nopan"
          onMouseDown={handleMouseDown(handleIndex)}
        >
          <div
            className={`
              bg-schemafy-bg border-2 rounded-full shadow-lg transition-all
              ${isActive ? 'scale-125 border-schemafy-blue' : 'border-schemafy-dark-gray hover:border-schemafy-blue'}
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
                isActive ? 'text-schemafy-blue' : 'text-schemafy-dark-gray'
              }
            />
          </div>
        </div>
      </EdgeLabelRenderer>
    );
  };

  const renderLabel = () => {
    if (!label) return null;

    return (
      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelPosition.x}px, ${labelPosition.y}px)`,
            fontSize: labelStyle?.fontSize || 12,
            fontWeight: labelStyle?.fontWeight || 'bold',
            color: labelStyle?.color || 'var(--color-schemafy-dark-gray)',
            pointerEvents: 'none',
            background: 'var(--color-schemafy-bg)',
            padding: '2px 6px',
            borderRadius: '4px',
            marginTop: '-25px',
          }}
        >
          {label}
        </div>
      </EdgeLabelRenderer>
    );
  };

  return (
    <>
      <BaseEdge
        path={path}
        markerEnd={markerEnd}
        markerStart={markerStart}
        style={style}
      />

      {renderLabel()}
      {renderHandle(handle1Position, 1)}
      {handle2Position && renderHandle(handle2Position, 2)}
    </>
  );
};
