import { type Node } from '@xyflow/react';
import type { Memo as ApiMemo, MemoPosition } from '@/features/memo/api/types';
import { z } from 'zod';

export interface MemoData extends Record<string, unknown> {
  content: string;
  comments: ApiMemo['comments'];
}

const PositionSchema = z.object({
  x: z.number(),
  y: z.number(),
});

export const safeParsePosition = (positions: MemoPosition): MemoPosition => {
  if (!positions) return { x: 0, y: 0 };
  const result = PositionSchema.safeParse(positions);
  if (result.success) {
    return result.data;
  }
  return { x: 0, y: 0 };
};

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
