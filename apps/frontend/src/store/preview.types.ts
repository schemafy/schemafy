import type {
  TableSnapshotResponse,
  RelationshipSnapshotResponse,
} from '@/features/drawing/api';
import type { RelationshipExtra } from '@/features/drawing/types';

export type PreviewKind = 'TABLE' | 'RELATIONSHIP';
export type PositionPreviewKind = 'TABLE_POSITION';
export type ExtraPreviewKind = 'RELATIONSHIP_EXTRA';

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

export type TablePositionPreviewEntry = PreviewEntryBase & {
  kind: 'TABLE_POSITION';
  tableId: string;
  position: { x: number; y: number };
};

export type RelationshipExtraPreviewEntry = PreviewEntryBase & {
  kind: 'RELATIONSHIP_EXTRA';
  relationshipId: string;
  extra: RelationshipExtra;
};

export type PreviewEntry =
  | TablePreviewEntry
  | RelationshipPreviewEntry
  | TablePositionPreviewEntry
  | RelationshipExtraPreviewEntry;
