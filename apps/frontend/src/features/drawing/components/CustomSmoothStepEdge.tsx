import { useState, useEffect } from 'react';
import {
  BaseEdge,
  EdgeLabelRenderer,
  useReactFlow,
  Position,
  type EdgeProps,
} from '@xyflow/react';
import { Move } from 'lucide-react';
import {
  type CrossDirectionControlPoints,
  type SameDirectionControlPoints,
  type Point,
} from '../types';

const isHorizontalPosition = (position: Position) =>
  position === Position.Left || position === Position.Right;

const getDefaultControlPoints = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  isCrossDirection: boolean,
) => {
  if (isCrossDirection) {
    return {
      controlPoint1X: sourceIsHorizontal ? (sourceX + targetX) / 2 : sourceX,
      controlPoint1Y: sourceIsHorizontal ? sourceY : (sourceY + targetY) / 2,
      controlPoint2X: sourceIsHorizontal ? (sourceX + targetX) / 2 : targetX,
      controlPoint2Y: (sourceY + targetY) / 2,
    };
  } else {
    const midX = (sourceX + targetX) / 2;
    const midY = (sourceY + targetY) / 2;
    return {
      controlPoint1X: midX,
      controlPoint1Y: midY,
      controlPoint2X: midX,
      controlPoint2Y: midY,
    };
  }
};

const getCurrentControlPoints = (
  data: EdgeProps['data'],
  defaults: CrossDirectionControlPoints,
) => ({
  controlPoint1X:
    typeof data?.controlPoint1X === 'number'
      ? data.controlPoint1X
      : defaults.controlPoint1X,
  controlPoint1Y:
    typeof data?.controlPoint1Y === 'number'
      ? data.controlPoint1Y
      : defaults.controlPoint1Y,
  controlPoint2X:
    typeof data?.controlPoint2X === 'number'
      ? data.controlPoint2X
      : defaults.controlPoint2X,
  controlPoint2Y:
    typeof data?.controlPoint2Y === 'number'
      ? data.controlPoint2Y
      : defaults.controlPoint2Y,
});

const buildCrossDirectionPath = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  cp: CrossDirectionControlPoints,
) => {
  if (sourceIsHorizontal) {
    return `M ${sourceX},${sourceY} L ${cp.controlPoint1X},${sourceY} L ${cp.controlPoint1X},${cp.controlPoint2Y} L ${targetX},${cp.controlPoint2Y} L ${targetX},${targetY}`;
  } else {
    return `M ${sourceX},${sourceY} L ${sourceX},${cp.controlPoint1Y} L ${cp.controlPoint2X},${cp.controlPoint1Y} L ${cp.controlPoint2X},${targetY} L ${targetX},${targetY}`;
  }
};

const buildSameDirectionPath = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  cp: SameDirectionControlPoints,
) => {
  if (sourceIsHorizontal) {
    return `M ${sourceX},${sourceY} L ${cp.controlPoint1X},${sourceY} L ${cp.controlPoint1X},${targetY} L ${targetX},${targetY}`;
  } else {
    return `M ${sourceX},${sourceY} L ${sourceX},${cp.controlPoint1Y} L ${targetX},${cp.controlPoint1Y} L ${targetX},${targetY}`;
  }
};

const getCrossDirectionHandles = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  cp: CrossDirectionControlPoints,
) => {
  if (sourceIsHorizontal) {
    return {
      handle1Position: {
        x: cp.controlPoint1X,
        y: (sourceY + cp.controlPoint2Y) / 2,
      },
      handle2Position: {
        x: (cp.controlPoint1X + targetX) / 2,
        y: cp.controlPoint2Y,
      },
    };
  } else {
    return {
      handle1Position: {
        x: (sourceX + cp.controlPoint2X) / 2,
        y: cp.controlPoint1Y,
      },
      handle2Position: {
        x: cp.controlPoint2X,
        y: (cp.controlPoint1Y + targetY) / 2,
      },
    };
  }
};

const getSameDirectionHandle = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  cp: SameDirectionControlPoints,
) => {
  if (sourceIsHorizontal) {
    return {
      x: cp.controlPoint1X,
      y: (sourceY + targetY) / 2,
    };
  } else {
    return {
      x: (sourceX + targetX) / 2,
      y: cp.controlPoint1Y,
    };
  }
};

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
        controlPoints,
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
    } else {
      const edgePath = buildSameDirectionPath(
        sourceX,
        sourceY,
        targetX,
        targetY,
        sourceIsHorizontal,
        controlPoints,
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
    }
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

    const calculateNewControlPoints = (
      flowPosition: Point,
      handleIndex: number,
    ) => {
      if (isCrossDirection) {
        if (handleIndex === 1) {
          return {
            controlPoint1X: sourceIsHorizontal
              ? flowPosition.x
              : controlPoint1X,
            controlPoint1Y: sourceIsHorizontal
              ? controlPoint1Y
              : flowPosition.y,
            controlPoint2X,
            controlPoint2Y,
          };
        } else {
          return {
            controlPoint1X,
            controlPoint1Y,
            controlPoint2X: sourceIsHorizontal
              ? controlPoint2X
              : flowPosition.x,
            controlPoint2Y: sourceIsHorizontal
              ? flowPosition.y
              : controlPoint2Y,
          };
        }
      } else {
        const newPoint1X = sourceIsHorizontal ? flowPosition.x : controlPoint1X;
        const newPoint1Y = sourceIsHorizontal ? controlPoint1Y : flowPosition.y;
        return {
          controlPoint1X: newPoint1X,
          controlPoint1Y: newPoint1Y,
          controlPoint2X: newPoint1X,
          controlPoint2Y: newPoint1Y,
        };
      }
    };

    const updateControlPointsInEdges = (
      newPoints: CrossDirectionControlPoints,
    ) => {
      setEdges((edges) =>
        edges.map((edge) => {
          if (edge.id !== id) return edge;
          return {
            ...edge,
            data: {
              ...edge.data,
              ...newPoints,
            },
          };
        }),
      );
    };

    const handleMouseMove = (e: MouseEvent) => {
      const flowPosition = screenToFlowPosition({
        x: e.clientX,
        y: e.clientY,
      });

      const newControlPoints = calculateNewControlPoints(
        flowPosition,
        draggingHandle,
      );

      finalControlPoints = newControlPoints;
      updateControlPointsInEdges(newControlPoints);
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
