import { type ConnectionLineComponentProps } from '@xyflow/react';
import type { Point } from '../types';
import {
  getFloatingAnchorPoint,
  getInternalNodeRect,
} from '../utils/anchorGeometry';
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
  fromNode,
  toNode,
}: ConnectionLineComponentProps) => {
  const fromRect = getInternalNodeRect(fromNode);
  const toRect = getInternalNodeRect(toNode ?? undefined);
  const cursorRect = {
    x: toX,
    y: toY,
    width: 1,
    height: 1,
  };

  const sourceAnchor = fromRect
    ? getFloatingAnchorPoint(fromRect, toRect ?? cursorRect)
    : null;
  const targetAnchor =
    fromRect && toRect ? getFloatingAnchorPoint(toRect, fromRect) : null;

  const effectiveFromPosition = sourceAnchor?.position ?? fromPosition;
  const effectiveToPosition = targetAnchor?.position ?? toPosition;
  const sourceIsHorizontal = isHorizontalPosition(effectiveFromPosition);
  const targetIsHorizontal = isHorizontalPosition(effectiveToPosition);
  const isCrossDirection = sourceIsHorizontal !== targetIsHorizontal;

  const source: Point = {
    x: sourceAnchor?.point.x ?? fromX,
    y: sourceAnchor?.point.y ?? fromY,
  };
  const target: Point = {
    x: targetAnchor?.point.x ?? toX,
    y: targetAnchor?.point.y ?? toY,
  };

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
