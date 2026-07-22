import type { AxiosRequestConfig } from 'axios';

import type { MutationResponse } from './types';
import { collaborationStore } from '@/store/collaboration.store';
import { operationHistoryStore } from '@/store/operation-history.store';
import { queryClient } from '@/lib/config/query-client';

export type CommittedMutationResult = Pick<
  MutationResponse<unknown>,
  'operation' | 'affectedTableIds' | 'requestClientOperationId'
>;

const UNDO_REDO_IN_PROGRESS = 'UNDO_REDO_IN_PROGRESS';

export const isUndoRedoInProgressError = (error: unknown) =>
  error instanceof Error && error.message === UNDO_REDO_IN_PROGRESS;

export const createErdMutationConfig = (
  schemaId?: string,
): AxiosRequestConfig => {
  if (schemaId && queryClient.isMutating() > 0) {
    throw new Error(UNDO_REDO_IN_PROGRESS);
  }

  const clientOperationId = crypto.randomUUID();
  const baseSchemaRevision = schemaId
    ? collaborationStore.getSchemaRevision(schemaId)
    : null;
  const headers: Record<string, string> = {
    'X-Client-Op-Id': clientOperationId,
  };

  if (baseSchemaRevision !== null) {
    headers['X-Base-Schema-Revision'] = String(baseSchemaRevision);
  }

  operationHistoryStore.markPending({
    clientOperationId,
    schemaId: schemaId ?? null,
    baseSchemaRevision,
  });

  return { headers };
};

export const syncCommittedRevision = (
  schemaId: string,
  result: CommittedMutationResult,
) => {
  if (!result.operation) {
    if (result.requestClientOperationId) {
      operationHistoryStore.markNoOp(result.requestClientOperationId);
    }
    return 'applied';
  }

  const committedRevision = result.operation.committedRevision;
  const syncStatus = collaborationStore.getRevisionSyncStatus(
    schemaId,
    committedRevision,
  );

  operationHistoryStore.markUndoable(
    schemaId,
    result.operation,
    result.affectedTableIds ?? [],
  );

  if (syncStatus === 'stale') return syncStatus;

  collaborationStore.setSchemaRevision(schemaId, committedRevision);

  return syncStatus;
};
