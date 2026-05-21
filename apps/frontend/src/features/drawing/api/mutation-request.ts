import type { AxiosRequestConfig } from 'axios';

import type { MutationResponse } from './types';
import { collaborationStore } from '@/store/collaboration.store';
import { operationHistoryStore } from '@/store/operation-history.store';

type CommittedMutationResult = Pick<
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

  operationHistoryStore.markUndoable(
    schemaId,
    result.operation,
    result.affectedTableIds ?? [],
  );

  const currentRevision = collaborationStore.getSchemaRevision(schemaId);

  if (currentRevision === null || committedRevision > currentRevision) {
    collaborationStore.setSchemaRevision(schemaId, committedRevision);
  }
};
