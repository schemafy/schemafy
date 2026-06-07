import { Position, type InternalNode } from '@xyflow/react';
import type { Point } from '../types';

const MIN_SIZE = 1;
const CENTER_MATCH_EPSILON = 0.0001;

type AnchorSide = 'top' | 'right' | 'bottom' | 'left';

export type Rect = {
  x: number;
  y: number;
  width: number;
  height: number;
};

type AnchorPoint = {
  point: Point;
  position: Position;
};

const clamp = (value: number, min: number, max: number) =>
  Math.min(Math.max(value, min), max);

const normalizeRect = (rect: Rect): Rect => ({
  x: rect.x,
  y: rect.y,
  width: Math.max(rect.width, MIN_SIZE),
  height: Math.max(rect.height, MIN_SIZE),
});

export const getInternalNodeRect = (
  node: InternalNode | undefined,
): Rect | null => {
  if (!node) return null;

  const width = node.measured?.width ?? node.width;
  const height = node.measured?.height ?? node.height;

  if (typeof width !== 'number' || typeof height !== 'number') {
    return null;
  }

  return normalizeRect({
    x: node.internals.positionAbsolute.x,
    y: node.internals.positionAbsolute.y,
    width,
    height,
  });
};

const getCenter = (rect: Rect): Point => {
  const safeRect = normalizeRect(rect);

  return {
    x: safeRect.x + safeRect.width / 2,
    y: safeRect.y + safeRect.height / 2,
  };
};

const toFlowPosition = (side: AnchorSide): Position => {
  switch (side) {
    case 'top':
      return Position.Top;
    case 'right':
      return Position.Right;
    case 'bottom':
      return Position.Bottom;
    case 'left':
      return Position.Left;
  }
};

const createAnchorPoint = (
  side: AnchorSide,
  point: Point,
): AnchorPoint => ({
  point,
  position: toFlowPosition(side),
});

export const resolveRelationshipAnchor = (
  anchorRect: Rect,
  peerRect: Rect,
): AnchorPoint => {
  const safeRect = normalizeRect(anchorRect);
  const anchorCenter = getCenter(safeRect);
  const peerCenter = getCenter(peerRect);
  const dx = peerCenter.x - anchorCenter.x;
  const dy = peerCenter.y - anchorCenter.y;

  if (
    Math.abs(dx) < CENTER_MATCH_EPSILON &&
    Math.abs(dy) < CENTER_MATCH_EPSILON
  ) {
    return createAnchorPoint('right', {
      x: safeRect.x + safeRect.width,
      y: anchorCenter.y,
    });
  }

  const halfWidth = safeRect.width / 2;
  const halfHeight = safeRect.height / 2;
  const hitsLeftOrRightSide =
    Math.abs(dx) / halfWidth >= Math.abs(dy) / halfHeight;

  if (hitsLeftOrRightSide) {
    const side: AnchorSide = dx >= 0 ? 'right' : 'left';
    const x = side === 'right' ? safeRect.x + safeRect.width : safeRect.x;
    const y = clamp(
      anchorCenter.y + dy * (halfWidth / Math.abs(dx)),
      safeRect.y,
      safeRect.y + safeRect.height,
    );

    return createAnchorPoint(side, { x, y });
  }

  const side: AnchorSide = dy >= 0 ? 'bottom' : 'top';
  const x = clamp(
    anchorCenter.x + dx * (halfHeight / Math.abs(dy)),
    safeRect.x,
    safeRect.x + safeRect.width,
  );
  const y = side === 'bottom' ? safeRect.y + safeRect.height : safeRect.y;

  return createAnchorPoint(side, { x, y });
};
