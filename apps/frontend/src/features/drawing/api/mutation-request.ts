import type { AxiosRequestConfig } from 'axios';

import type { MutationResponse } from './types';
import { collaborationStore } from '@/store/collaboration.store';
import { operationHistoryStore } from '@/store/operation-history.store';

export type CommittedMutationResult = Pick<
  MutationResponse<unknown>,
  'operation' | 'affectedTableIds'
>;

export const createErdMutationConfig = (
  schemaId?: string,
): AxiosRequestConfig => {
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
