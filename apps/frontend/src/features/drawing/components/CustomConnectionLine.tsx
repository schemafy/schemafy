import { type ConnectionLineComponentProps } from '@xyflow/react';
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

  const controlPoints = getDefaultControlPoints(
    fromX,
    fromY,
    toX,
    toY,
    sourceIsHorizontal,
    isCrossDirection,
  );

  const path = isCrossDirection
    ? buildCrossDirectionPath(
        fromX,
        fromY,
        toX,
        toY,
        sourceIsHorizontal,
        controlPoints.controlPoint1X,
        controlPoints.controlPoint1Y,
        controlPoints.controlPoint2X,
        controlPoints.controlPoint2Y,
      )
    : buildSameDirectionPath(
        fromX,
        fromY,
        toX,
        toY,
        sourceIsHorizontal,
        controlPoints.controlPoint1X,
        controlPoints.controlPoint1Y,
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
