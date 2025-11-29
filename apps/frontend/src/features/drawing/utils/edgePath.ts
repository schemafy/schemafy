import { Position, type EdgeProps } from '@xyflow/react';
import {
  type CrossDirectionControlPoints,
  type SameDirectionControlPoints,
  type Point,
} from '../types';

export const isHorizontalPosition = (position: Position) =>
  position === Position.Left || position === Position.Right;

export const buildCrossDirectionPath = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  controlPoint1X: number,
  controlPoint1Y: number,
  controlPoint2X: number,
  controlPoint2Y: number,
): string => {
  if (sourceIsHorizontal) {
    return `M ${sourceX},${sourceY} L ${controlPoint1X},${sourceY} L ${controlPoint1X},${controlPoint2Y} L ${targetX},${controlPoint2Y} L ${targetX},${targetY}`;
  } else {
    return `M ${sourceX},${sourceY} L ${sourceX},${controlPoint1Y} L ${controlPoint2X},${controlPoint1Y} L ${controlPoint2X},${targetY} L ${targetX},${targetY}`;
  }
};

export const buildSameDirectionPath = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  controlPoint1X: number,
  controlPoint1Y: number,
): string => {
  if (sourceIsHorizontal) {
    return `M ${sourceX},${sourceY} L ${controlPoint1X},${sourceY} L ${controlPoint1X},${targetY} L ${targetX},${targetY}`;
  } else {
    return `M ${sourceX},${sourceY} L ${sourceX},${controlPoint1Y} L ${targetX},${controlPoint1Y} L ${targetX},${targetY}`;
  }
};

export const getDefaultControlPoints = (
  sourceX: number,
  sourceY: number,
  targetX: number,
  targetY: number,
  sourceIsHorizontal: boolean,
  isCrossDirection: boolean,
) => {
  if (isCrossDirection) {
    const midY = (sourceY + targetY) / 2;
    return {
      controlPoint1X: sourceIsHorizontal ? (sourceX + targetX) / 2 : sourceX,
      controlPoint1Y: sourceIsHorizontal ? sourceY : midY,
      controlPoint2X: sourceIsHorizontal ? (sourceX + targetX) / 2 : targetX,
      controlPoint2Y: midY,
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

export const getCurrentControlPoints = (
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

export const getCrossDirectionHandles = (
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
  }

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
};

export const getSameDirectionHandle = (
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
  }

  return {
    x: (sourceX + targetX) / 2,
    y: cp.controlPoint1Y,
  };
};

export const calculateNewControlPoints = (
  flowPosition: Point,
  handleIndex: number,
  isCrossDirection: boolean,
  sourceIsHorizontal: boolean,
  currentControlPoints: CrossDirectionControlPoints,
): CrossDirectionControlPoints => {
  const { controlPoint1X, controlPoint1Y, controlPoint2X, controlPoint2Y } =
    currentControlPoints;

  if (isCrossDirection) {
    if (handleIndex === 1) {
      return {
        controlPoint1X: sourceIsHorizontal ? flowPosition.x : controlPoint1X,
        controlPoint1Y: sourceIsHorizontal ? controlPoint1Y : flowPosition.y,
        controlPoint2X,
        controlPoint2Y,
      };
    }

    return {
      controlPoint1X,
      controlPoint1Y,
      controlPoint2X: sourceIsHorizontal ? controlPoint2X : flowPosition.x,
      controlPoint2Y: sourceIsHorizontal ? flowPosition.y : controlPoint2Y,
    };
  }

  const newPoint1X = sourceIsHorizontal ? flowPosition.x : controlPoint1X;
  const newPoint1Y = sourceIsHorizontal ? controlPoint1Y : flowPosition.y;

  return {
    controlPoint1X: newPoint1X,
    controlPoint1Y: newPoint1Y,
    controlPoint2X: newPoint1X,
    controlPoint2Y: newPoint1Y,
  };
};
