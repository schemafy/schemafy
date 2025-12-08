import { type ConnectionLineComponentProps } from '@xyflow/react';
import type { Point } from '../types';
import {
  isHorizontalPosition,
  getDefaultControlPoints,
  buildCrossDirectionPath,
  buildSameDirectionPath,
} from '../utils/edgePath';

export const CustomConnectionLine = ({
  fromX,
  fromY,
  toX,
  toY,
  fromPosition,
  toPosition,
}: ConnectionLineComponentProps) => {
  const sourceIsHorizontal = isHorizontalPosition(fromPosition);
  const targetIsHorizontal = isHorizontalPosition(toPosition);
  const isCrossDirection = sourceIsHorizontal !== targetIsHorizontal;

  const source: Point = { x: fromX, y: fromY };
  const target: Point = { x: toX, y: toY };

  const controlPoints = getDefaultControlPoints(
    source,
    target,
    sourceIsHorizontal,
    isCrossDirection,
  );

  const path = isCrossDirection
    ? buildCrossDirectionPath(
        source,
        target,
        sourceIsHorizontal,
        controlPoints.controlPoint1,
        controlPoints.controlPoint2,
      )
    : buildSameDirectionPath(
        source,
        target,
        sourceIsHorizontal,
        controlPoints.controlPoint1,
      );

  return (
    <g>
      <path
        fill="none"
        stroke="var(--color-schemafy-dark-gray)"
        strokeWidth={2}
        strokeOpacity={0.5}
        d={path}
      />
    </g>
  );
};
