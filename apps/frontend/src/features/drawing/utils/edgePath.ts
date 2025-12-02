import { Position } from '@xyflow/react';
import {
  type CrossDirectionControlPoints,
  type SameDirectionControlPoints,
  type Point,
  type EdgeData,
} from '../types';

export const isHorizontalPosition = (position: Position) =>
  position === Position.Left || position === Position.Right;

export const buildCrossDirectionPath = (
  source: Point,
  target: Point,
  sourceIsHorizontal: boolean,
  controlPoint1: Point,
  controlPoint2: Point,
) => {
  if (sourceIsHorizontal) {
    return `M ${source.x},${source.y} L ${controlPoint1.x},${source.y} L ${controlPoint1.x},${controlPoint2.y} L ${target.x},${controlPoint2.y} L ${target.x},${target.y}`;
  } else {
    return `M ${source.x},${source.y} L ${source.x},${controlPoint1.y} L ${controlPoint2.x},${controlPoint1.y} L ${controlPoint2.x},${target.y} L ${target.x},${target.y}`;
  }
};

export const buildSameDirectionPath = (
  source: Point,
  target: Point,
  sourceIsHorizontal: boolean,
  controlPoint1: Point,
) => {
  if (sourceIsHorizontal) {
    return `M ${source.x},${source.y} L ${controlPoint1.x},${source.y} L ${controlPoint1.x},${target.y} L ${target.x},${target.y}`;
  } else {
    return `M ${source.x},${source.y} L ${source.x},${controlPoint1.y} L ${target.x},${controlPoint1.y} L ${target.x},${target.y}`;
  }
};

export const getDefaultControlPoints = (
  source: Point,
  target: Point,
  sourceIsHorizontal: boolean,
  isCrossDirection: boolean,
) => {
  const midPoint = {
    x: (source.x + target.x) / 2,
    y: (source.y + target.y) / 2,
  };
  if (isCrossDirection) {
    return {
      controlPoint1: {
        x: sourceIsHorizontal ? midPoint.x : source.x,
        y: sourceIsHorizontal ? source.y : midPoint.y,
      },
      controlPoint2: {
        x: sourceIsHorizontal ? midPoint.x : target.x,
        y: midPoint.y,
      },
    };
  } else {
    return {
      controlPoint1: midPoint,
      controlPoint2: midPoint,
    };
  }
};

const getCurrentControlPoints = (
  defaults: CrossDirectionControlPoints,
  data?: EdgeData,
) => {
  return {
    controlPoint1: data?.controlPoint1 ?? defaults.controlPoint1,
    controlPoint2: data?.controlPoint2 ?? defaults.controlPoint2,
  };
};

const getCrossDirectionHandles = (
  source: Point,
  target: Point,
  sourceIsHorizontal: boolean,
  cp: CrossDirectionControlPoints,
) => {
  if (sourceIsHorizontal) {
    const verticalMid = (source.y + cp.controlPoint2.y) / 2;
    const horizontalMid = (cp.controlPoint1.x + target.x) / 2;

    return {
      handle1Position: { x: cp.controlPoint1.x, y: verticalMid },
      handle2Position: { x: horizontalMid, y: cp.controlPoint2.y },
    };
  } else {
    const horizontalMid = (source.x + cp.controlPoint2.x) / 2;
    const verticalMid = (cp.controlPoint1.y + target.y) / 2;

    return {
      handle1Position: { x: horizontalMid, y: cp.controlPoint1.y },
      handle2Position: { x: cp.controlPoint2.x, y: verticalMid },
    };
  }
};

const getSameDirectionHandle = (
  source: Point,
  target: Point,
  sourceIsHorizontal: boolean,
  cp: SameDirectionControlPoints,
) => {
  if (sourceIsHorizontal) {
    return {
      x: cp.controlPoint1.x,
      y: (source.y + target.y) / 2,
    };
  } else {
    return {
      x: (source.x + target.x) / 2,
      y: cp.controlPoint1.y,
    };
  }
};

export const calculateEdgeGeometry = (
  source: Point,
  target: Point,
  sourceIsHorizontal: boolean,
  isCrossDirection: boolean,
  data?: unknown,
) => {
  const edgeData = data as EdgeData | undefined;

  const defaults = getDefaultControlPoints(
    source,
    target,
    sourceIsHorizontal,
    isCrossDirection,
  );

  const controlPoints = getCurrentControlPoints(defaults, edgeData);

  if (isCrossDirection) {
    const path = buildCrossDirectionPath(
      source,
      target,
      sourceIsHorizontal,
      controlPoints.controlPoint1,
      controlPoints.controlPoint2,
    );

    const handles = getCrossDirectionHandles(
      source,
      target,
      sourceIsHorizontal,
      controlPoints,
    );

    return {
      controlPoints,
      path,
      ...handles,
    };
  } else {
    const path = buildSameDirectionPath(
      source,
      target,
      sourceIsHorizontal,
      controlPoints.controlPoint1,
    );

    const handle1Position = getSameDirectionHandle(
      source,
      target,
      sourceIsHorizontal,
      controlPoints,
    );

    return {
      controlPoints,
      path,
      handle1Position,
      handle2Position: null,
    };
  }
};

export const calculateNewControlPoints = (
  flowPosition: Point,
  handleIndex: number,
  isCrossDirection: boolean,
  sourceIsHorizontal: boolean,
  currentControlPoints: CrossDirectionControlPoints,
) => {
  const { controlPoint1, controlPoint2 } = currentControlPoints;

  if (isCrossDirection) {
    if (handleIndex === 1) {
      return {
        controlPoint1: {
          x: sourceIsHorizontal ? flowPosition.x : controlPoint1.x,
          y: sourceIsHorizontal ? controlPoint1.y : flowPosition.y,
        },
        controlPoint2,
      };
    }

    return {
      controlPoint1,
      controlPoint2: {
        x: sourceIsHorizontal ? controlPoint2.x : flowPosition.x,
        y: sourceIsHorizontal ? flowPosition.y : controlPoint2.y,
      },
    };
  } else {
    const newPoint: Point = {
      x: sourceIsHorizontal ? flowPosition.x : controlPoint1.x,
      y: sourceIsHorizontal ? controlPoint1.y : flowPosition.y,
    };

    return {
      controlPoint1: newPoint,
      controlPoint2: newPoint,
    };
  }
};
