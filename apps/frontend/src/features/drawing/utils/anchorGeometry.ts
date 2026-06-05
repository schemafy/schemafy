import { Position, type InternalNode } from '@xyflow/react';
import type {
  AnchorSide,
  FixedAnchor,
  Point,
  RelationshipAnchor,
} from '../types';

const MIN_SIZE = 1;
const CENTER_MATCH_EPSILON = 0.0001;

export type Rect = {
  x: number;
  y: number;
  width: number;
  height: number;
};

export type AnchorPoint = {
  point: Point;
  side: AnchorSide;
  position: Position;
  ratio: number;
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

const getSideRatio = (rect: Rect, side: AnchorSide, point: Point) => {
  if (side === 'top' || side === 'bottom') {
    return clamp((point.x - rect.x) / rect.width, 0, 1);
  }

  return clamp((point.y - rect.y) / rect.height, 0, 1);
};

const createAnchorPoint = (
  rect: Rect,
  side: AnchorSide,
  point: Point,
): AnchorPoint => ({
  point,
  side,
  position: toFlowPosition(side),
  ratio: getSideRatio(rect, side, point),
});

const resolveFixedAnchor = (
  rect: Rect,
  anchor: FixedAnchor,
): AnchorPoint => {
  const safeRect = normalizeRect(rect);
  const ratio = clamp(anchor.ratio, 0, 1);

  switch (anchor.side) {
    case 'top':
      return createAnchorPoint(safeRect, anchor.side, {
        x: safeRect.x + safeRect.width * ratio,
        y: safeRect.y,
      });
    case 'right':
      return createAnchorPoint(safeRect, anchor.side, {
        x: safeRect.x + safeRect.width,
        y: safeRect.y + safeRect.height * ratio,
      });
    case 'bottom':
      return createAnchorPoint(safeRect, anchor.side, {
        x: safeRect.x + safeRect.width * ratio,
        y: safeRect.y + safeRect.height,
      });
    case 'left':
      return createAnchorPoint(safeRect, anchor.side, {
        x: safeRect.x,
        y: safeRect.y + safeRect.height * ratio,
      });
  }
};

const resolveFloatingAnchor = (
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
    return resolveFixedAnchor(safeRect, {
      mode: 'fixed',
      side: 'right',
      ratio: 0.5,
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

    return createAnchorPoint(safeRect, side, { x, y });
  }

  const side: AnchorSide = dy >= 0 ? 'bottom' : 'top';
  const x = clamp(
    anchorCenter.x + dx * (halfHeight / Math.abs(dy)),
    safeRect.x,
    safeRect.x + safeRect.width,
  );
  const y = side === 'bottom' ? safeRect.y + safeRect.height : safeRect.y;

  return createAnchorPoint(safeRect, side, { x, y });
};

export const resolveRelationshipAnchor = (
  anchorRect: Rect,
  peerRect: Rect,
  anchor: RelationshipAnchor | undefined,
): AnchorPoint => {
  if (anchor?.mode === 'fixed') {
    return resolveFixedAnchor(anchorRect, anchor);
  }

  return resolveFloatingAnchor(anchorRect, peerRect);
};
