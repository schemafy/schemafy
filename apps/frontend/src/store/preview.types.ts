import type {
  TableSnapshotResponse,
  RelationshipSnapshotResponse,
} from '@/features/drawing/api';

export type PreviewKind = 'TABLE' | 'RELATIONSHIP';

export type PreviewEntryBase = {
  previewId: string;
  schemaId: string;
  sessionId: string;
  createdAt: number;
  ttlMs: number | null;
};

export type TablePreviewEntry = PreviewEntryBase & {
  kind: 'TABLE';
  snapshot: TableSnapshotResponse;
};

export type RelationshipPreviewEntry = PreviewEntryBase & {
  kind: 'RELATIONSHIP';
  snapshot: RelationshipSnapshotResponse;
  fkTableId: string;
  pkTableId: string;
};

export type PreviewEntry = TablePreviewEntry | RelationshipPreviewEntry;
