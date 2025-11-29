import { useState, useEffect } from 'react';
import {
  BaseEdge,
  EdgeLabelRenderer,
  useReactFlow,
  type EdgeProps,
} from '@xyflow/react';
import { Move } from 'lucide-react';
import type { CrossDirectionControlPoints, Point } from '../types';
import {
  isHorizontalPosition,
  getDefaultControlPoints,
  buildCrossDirectionPath,
  buildSameDirectionPath,
  getCrossDirectionHandles,
  getCurrentControlPoints,
  calculateNewControlPoints,
  getSameDirectionHandle,
} from '../utils/edgePath';

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
}: EdgeProps) => {
  const { setEdges, screenToFlowPosition } = useReactFlow();
  const [draggingHandle, setDraggingHandle] = useState<number | null>(null);

  const sourceIsHorizontal = isHorizontalPosition(sourcePosition);
  const targetIsHorizontal = isHorizontalPosition(targetPosition);
  const isCrossDirection = sourceIsHorizontal !== targetIsHorizontal;

  const {
    controlPoint1X,
    controlPoint1Y,
    controlPoint2X,
    controlPoint2Y,
    path,
    handle1Position,
    handle2Position,
  } = (() => {
    const defaults = getDefaultControlPoints(
      sourceX,
      sourceY,
      targetX,
      targetY,
      sourceIsHorizontal,
      isCrossDirection,
    );

    const controlPoints = getCurrentControlPoints(data, defaults);

    if (isCrossDirection) {
      const edgePath = buildCrossDirectionPath(
        sourceX,
        sourceY,
        targetX,
        targetY,
        sourceIsHorizontal,
        controlPoints.controlPoint1X,
        controlPoints.controlPoint1Y,
        controlPoints.controlPoint2X,
        controlPoints.controlPoint2Y,
      );

      const handles = getCrossDirectionHandles(
        sourceX,
        sourceY,
        targetX,
        targetY,
        sourceIsHorizontal,
        controlPoints,
      );

      return {
        ...controlPoints,
        path: edgePath,
        ...handles,
      };
    }

    const edgePath = buildSameDirectionPath(
      sourceX,
      sourceY,
      targetX,
      targetY,
      sourceIsHorizontal,
      controlPoints.controlPoint1X,
      controlPoints.controlPoint1Y,
    );

    const handlePosition = getSameDirectionHandle(
      sourceX,
      sourceY,
      targetX,
      targetY,
      sourceIsHorizontal,
      controlPoints,
    );

    return {
      ...controlPoints,
      path: edgePath,
      handle1Position: handlePosition,
      handle2Position: null,
    };
  })();

  const handleMouseDown = (handleIndex: number) => (e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    setDraggingHandle(handleIndex);
  };

  useEffect(() => {
    if (draggingHandle === null) return;

    let finalControlPoints: CrossDirectionControlPoints = {
      controlPoint1X,
      controlPoint1Y,
      controlPoint2X,
      controlPoint2Y,
    };

    const handleMouseMove = (e: MouseEvent) => {
      const flowPosition = screenToFlowPosition({
        x: e.clientX,
        y: e.clientY,
      });

      const newControlPoints = calculateNewControlPoints(
        flowPosition,
        draggingHandle,
        isCrossDirection,
        sourceIsHorizontal,
        finalControlPoints,
      );

      finalControlPoints = newControlPoints;

      setEdges((edges) =>
        edges.map((edge) =>
          edge.id === id
            ? { ...edge, data: { ...edge.data, ...newControlPoints } }
            : edge,
        ),
      );
    };

    const handleMouseUp = () => {
      setDraggingHandle(null);
      if (data && typeof data.onControlPointDragEnd === 'function') {
        data.onControlPointDragEnd(
          id,
          finalControlPoints.controlPoint1X,
          finalControlPoints.controlPoint1Y,
          finalControlPoints.controlPoint2X,
          finalControlPoints.controlPoint2Y,
        );
      }
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [
    draggingHandle,
    id,
    data,
    setEdges,
    screenToFlowPosition,
    isCrossDirection,
    sourceIsHorizontal,
    controlPoint1X,
    controlPoint1Y,
    controlPoint2X,
    controlPoint2Y,
  ]);

  const labelPosition: Point = isCrossDirection
    ? {
        x: sourceIsHorizontal ? controlPoint1X : controlPoint2X,
        y: sourceIsHorizontal ? controlPoint2Y : controlPoint1Y,
      }
    : handle1Position;

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
            background: 'white',
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
