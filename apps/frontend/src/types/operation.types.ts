export type LocalOperationStatus =
  | 'pending'
  | 'committed'
  | 'undoable'
  | 'failed'
  | 'superseded';

export type LocalOperationMetadata = {
  clientOperationId: string;
  schemaId: string | null;
  baseSchemaRevision: number | null;
  opId: string | null;
  committedRevision: number | null;
  derivationKind: string | null;
  status: LocalOperationStatus;
  affectedTableIds: string[];
  failureMessage: string | null;
  createdAt: number;
  committedAt: number | null;
};

export type PendingOperationMetadata = Pick<
  LocalOperationMetadata,
  'clientOperationId' | 'schemaId' | 'baseSchemaRevision'
>;