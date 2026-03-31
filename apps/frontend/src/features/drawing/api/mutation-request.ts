import type { AxiosRequestConfig } from 'axios';

import type { MutationResponse } from './types';
import { collaborationStore } from '@/store/collaboration.store';

export const createErdMutationConfig = (
  schemaId?: string,
): AxiosRequestConfig => {
  const headers: Record<string, string> = {
    'X-Client-Op-Id': crypto.randomUUID(),
  };

  if (schemaId) {
    const baseSchemaRevision = collaborationStore.getSchemaRevision(schemaId);

    if (baseSchemaRevision !== null) {
      headers['X-Base-Schema-Revision'] = String(baseSchemaRevision);
    }
  }

  return { headers };
};

export const syncCommittedRevision = (
  schemaId: string,
  result: Pick<MutationResponse, 'operation'>,
) => {
  collaborationStore.setSchemaRevision(
    schemaId,
    result.operation.committedRevision,
  );
};
