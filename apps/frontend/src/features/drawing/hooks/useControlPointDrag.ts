import { useState, useEffect, useRef } from 'react';
import { useReactFlow, type Position } from '@xyflow/react';
import type { CrossDirectionControlPoints, Point, EdgeData } from '../types';
import {
  isHorizontalPosition,
  calculateNewControlPoints,
  calculateEdgeGeometry,
} from '../utils/edgePath';

interface UseControlPointDragParams {
  id: string;
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
  sourcePosition: Position;
  targetPosition: Position;
  data: EdgeData | undefined;
}

export const useControlPointDrag = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  data,
}: UseControlPointDragParams) => {
  const { screenToFlowPosition } = useReactFlow();
  const [draggingHandle, setDraggingHandle] = useState<number | null>(null);
  const [dragControlPoints, setDragControlPoints] =
    useState<CrossDirectionControlPoints | null>(null);

  const sourceIsHorizontal = isHorizontalPosition(sourcePosition);
  const targetIsHorizontal = isHorizontalPosition(targetPosition);
  const isCrossDirection = sourceIsHorizontal !== targetIsHorizontal;

  const source: Point = { x: sourceX, y: sourceY };
  const target: Point = { x: targetX, y: targetY };

  const effectiveData: EdgeData | undefined = dragControlPoints
    ? { ...data!, ...dragControlPoints }
    : data;

  const { controlPoints, path, handle1Position, handle2Position } =
    calculateEdgeGeometry(
      source,
      target,
      sourceIsHorizontal,
      isCrossDirection,
      effectiveData,
    );

  const controlPointsRef = useRef(controlPoints);
  const dataRef = useRef(data);
  const screenToFlowPositionRef = useRef(screenToFlowPosition);
  const isCrossDirectionRef = useRef(isCrossDirection);
  const sourceIsHorizontalRef = useRef(sourceIsHorizontal);

  controlPointsRef.current = controlPoints;
  dataRef.current = data;
  screenToFlowPositionRef.current = screenToFlowPosition;
  isCrossDirectionRef.current = isCrossDirection;
  sourceIsHorizontalRef.current = sourceIsHorizontal;

  const handleMouseDown = (handleIndex: number) => (e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    setDraggingHandle(handleIndex);
  };

  useEffect(() => {
    if (draggingHandle === null) return;

    let finalControlPoints: CrossDirectionControlPoints = {
      ...controlPointsRef.current,
    };

    const handleMouseMove = (e: MouseEvent) => {
      const flowPosition = screenToFlowPositionRef.current({
        x: e.clientX,
        y: e.clientY,
      });

      const newControlPoints = calculateNewControlPoints(
        flowPosition,
        draggingHandle,
        isCrossDirectionRef.current,
        sourceIsHorizontalRef.current,
        finalControlPoints,
      );

      finalControlPoints = newControlPoints;
      setDragControlPoints(newControlPoints);
    };

    const handleMouseUp = () => {
      setDraggingHandle(null);
      setDragControlPoints(null);
      const data = dataRef.current;
      if (data && typeof data.onControlPointDragEnd === 'function') {
        data.onControlPointDragEnd(
          id,
          finalControlPoints.controlPoint1,
          finalControlPoints.controlPoint2,
        );
      }
    };

    document.body.style.userSelect = 'none';
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.body.style.userSelect = '';
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingHandle, id]);

  const labelPosition: Point = isCrossDirection
    ? {
        x: sourceIsHorizontal
          ? controlPoints.controlPoint1.x
          : controlPoints.controlPoint2.x,
        y: sourceIsHorizontal
          ? controlPoints.controlPoint2.y
          : controlPoints.controlPoint1.y,
      }
    : handle1Position;

  return {
    path,
    handle1Position,
    handle2Position,
    labelPosition,
    draggingHandle,
    handleMouseDown,
  };
};
