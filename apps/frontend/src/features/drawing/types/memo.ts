// TODO: validator에 추가되면 erdStore로 마이그레이션
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
    position: { x: number; y: number };
  };
}
