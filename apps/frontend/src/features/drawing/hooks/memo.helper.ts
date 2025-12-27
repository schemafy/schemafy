import { type Node } from '@xyflow/react';
import type { Memo as ApiMemo } from '@/lib/api/memo';

export interface MemoData extends Record<string, unknown> {
  content: string;
  comments: ApiMemo['comments'];
}

export const safeParsePosition = (
  positions: string,
): { x: number; y: number } => {
  if (!positions) return { x: 0, y: 0 };
  try {
    const parsed = JSON.parse(positions);
    if (
      parsed &&
      typeof parsed === 'object' &&
      typeof parsed.x === 'number' &&
      typeof parsed.y === 'number'
    ) {
      return { x: parsed.x, y: parsed.y };
    }
  } catch {
    console.error('Failed to parse positions');
  }
  return { x: 0, y: 0 };
};

export const stringifyPosition = (pos: { x: number; y: number }): string =>
  JSON.stringify({ x: pos.x, y: pos.y });

export const transformApiMemoToNode = (memo: ApiMemo): Node<MemoData> => {
  const position = safeParsePosition(memo.positions);
  const lastCommentBody =
    memo.comments && memo.comments.length > 0
      ? (memo.comments[memo.comments.length - 1].body ?? '')
      : '';
  return {
    id: memo.id,
    type: 'memo',
    position,
    data: {
      content: lastCommentBody,
      comments: memo.comments,
    },
  };
};
