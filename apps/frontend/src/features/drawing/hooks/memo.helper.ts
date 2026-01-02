import { type Node } from '@xyflow/react';
import type { Memo as ApiMemo } from '@/lib/api/memo';
import { z } from 'zod';

export interface MemoData extends Record<string, unknown> {
  content: string;
  comments: ApiMemo['comments'];
  deleteFunc?: () => void;
}

const PositionSchema = z.object({
  x: z.number(),
  y: z.number(),
});

export const safeParsePosition = (
  positions: string,
): { x: number; y: number } => {
  if (!positions) return { x: 0, y: 0 };
  try {
    const parsed = JSON.parse(positions);
    const result = PositionSchema.safeParse(parsed);
    if (result.success) {
      return result.data;
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
