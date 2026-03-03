import type { Point } from '@/features/drawing/types';

export interface Memo {
  id: string;
  schemaId: string;
  elementType: 'SCHEMA';
  elementId: string;
  userId: string;
  content: string;
  parentMemoId: string | null;
  resolvedAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
  extra?: {
    position: Point;
  };
}
