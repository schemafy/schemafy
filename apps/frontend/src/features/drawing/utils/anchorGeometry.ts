import { Position, type Node } from '@xyflow/react';
import type { AnchorSide, FixedAnchor, Point } from '../types';

const MIN_RECT_SIZE = 1;
const CENTER_EPSILON = 0.0001;

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

export type NodeRectSource = Pick<Node, 'position' | 'width' | 'height'> & {
  measured?: {
    width?: number;
    height?: number;
  };
};

const clamp = (value: number, min: number, max: number) =>
  Math.min(Math.max(value, min), max);

const normalizeRect = (rect: Rect): Rect => ({
  x: rect.x,
  y: rect.y,
  width: Math.max(rect.width, MIN_RECT_SIZE),
  height: Math.max(rect.height, MIN_RECT_SIZE),
});

export const getNodeRect = (node: NodeRectSource | undefined): Rect | null => {
  if (!node) return null;

  const width = node.measured?.width ?? node.width;
  const height = node.measured?.height ?? node.height;

  if (typeof width !== 'number' || typeof height !== 'number') {
    return null;
  }

  return normalizeRect({
    x: node.position.x,
    y: node.position.y,
    width,
    height,
  });
};

export const getRectCenter = (rect: Rect): Point => {
  const normalizedRect = normalizeRect(rect);

  return {
    x: normalizedRect.x + normalizedRect.width / 2,
    y: normalizedRect.y + normalizedRect.height / 2,
  };
};

export const sideToPosition = (side: AnchorSide): Position => {
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

const getRatioForPoint = (rect: Rect, side: AnchorSide, point: Point) => {
  if (side === 'top' || side === 'bottom') {
    return clamp((point.x - rect.x) / rect.width, 0, 1);
  }

  return clamp((point.y - rect.y) / rect.height, 0, 1);
};

const buildAnchorPoint = (
  rect: Rect,
  side: AnchorSide,
  point: Point,
): AnchorPoint => ({
  point,
  side,
  position: sideToPosition(side),
  ratio: getRatioForPoint(rect, side, point),
});

export const getFixedAnchorPoint = (
  rect: Rect,
  anchor: FixedAnchor,
): AnchorPoint => {
  const normalizedRect = normalizeRect(rect);
  const ratio = clamp(anchor.ratio, 0, 1);

  switch (anchor.side) {
    case 'top':
      return buildAnchorPoint(normalizedRect, anchor.side, {
        x: normalizedRect.x + normalizedRect.width * ratio,
        y: normalizedRect.y,
      });
    case 'right':
      return buildAnchorPoint(normalizedRect, anchor.side, {
        x: normalizedRect.x + normalizedRect.width,
        y: normalizedRect.y + normalizedRect.height * ratio,
      });
    case 'bottom':
      return buildAnchorPoint(normalizedRect, anchor.side, {
        x: normalizedRect.x + normalizedRect.width * ratio,
        y: normalizedRect.y + normalizedRect.height,
      });
    case 'left':
      return buildAnchorPoint(normalizedRect, anchor.side, {
        x: normalizedRect.x,
        y: normalizedRect.y + normalizedRect.height * ratio,
      });
  }
};

export const getFloatingAnchorPoint = (
  fromRect: Rect,
  toRect: Rect,
): AnchorPoint => {
  const normalizedFromRect = normalizeRect(fromRect);
  const fromCenter = getRectCenter(normalizedFromRect);
  const toCenter = getRectCenter(toRect);
  const dx = toCenter.x - fromCenter.x;
  const dy = toCenter.y - fromCenter.y;

  if (Math.abs(dx) < CENTER_EPSILON && Math.abs(dy) < CENTER_EPSILON) {
    return getFixedAnchorPoint(normalizedFromRect, {
      mode: 'fixed',
      side: 'right',
      ratio: 0.5,
    });
  }

  const halfWidth = normalizedFromRect.width / 2;
  const halfHeight = normalizedFromRect.height / 2;
  const hitsVerticalSide =
    Math.abs(dx) / halfWidth >= Math.abs(dy) / halfHeight;

  if (hitsVerticalSide) {
    const side: AnchorSide = dx >= 0 ? 'right' : 'left';
    const x =
      side === 'right'
        ? normalizedFromRect.x + normalizedFromRect.width
        : normalizedFromRect.x;
    const y = clamp(
      fromCenter.y + dy * (halfWidth / Math.abs(dx)),
      normalizedFromRect.y,
      normalizedFromRect.y + normalizedFromRect.height,
    );

    return buildAnchorPoint(normalizedFromRect, side, { x, y });
  }

  const side: AnchorSide = dy >= 0 ? 'bottom' : 'top';
  const x = clamp(
    fromCenter.x + dx * (halfHeight / Math.abs(dy)),
    normalizedFromRect.x,
    normalizedFromRect.x + normalizedFromRect.width,
  );
  const y =
    side === 'bottom'
      ? normalizedFromRect.y + normalizedFromRect.height
      : normalizedFromRect.y;

  return buildAnchorPoint(normalizedFromRect, side, { x, y });
};
