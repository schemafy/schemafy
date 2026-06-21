import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createSchema, changeSchemaName, deleteSchema } from '../api';
import type { CreateSchemaRequest, ChangeSchemaNameRequest } from '../api';
import { collaborationStore } from '@/store/collaboration.store';
import { operationHistoryStore } from '@/store/operation-history.store';
import { erdKeys } from './query-keys';
import { syncCommittedRevision } from '../api/mutation-request';

export const useCreateSchema = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateSchemaRequest) => createSchema(data),
    onSuccess: (result) => {
      const syncStatus = syncCommittedRevision(result.data.id, result);

      if (syncStatus === 'stale') return;

      queryClient.invalidateQueries({
        queryKey: erdKeys.schemas(projectId),
      });
    },
  });
};

export const useChangeSchemaName = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      schemaId,
      data,
    }: {
      schemaId: string;
      data: ChangeSchemaNameRequest;
    }) => changeSchemaName(schemaId, data),
    onSuccess: (result, { schemaId }) => {
      const syncStatus = syncCommittedRevision(schemaId, result);

      if (syncStatus === 'stale') return;

      queryClient.invalidateQueries({
        queryKey: erdKeys.schemas(projectId),
      });
    },
  });
};

export const useDeleteSchema = (projectId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (schemaId: string) => deleteSchema(schemaId),
    onSuccess: (result, deletedSchemaId) => {
      const syncStatus = syncCommittedRevision(deletedSchemaId, result);

      if (syncStatus === 'stale') return;

      queryClient.removeQueries({
        queryKey: erdKeys.schemaSnapshots(deletedSchemaId),
      });
      queryClient.invalidateQueries({
        queryKey: erdKeys.schemas(projectId),
      });
      collaborationStore.clearSchemaRevision(deletedSchemaId);
      operationHistoryStore.clearSchemaHistory(deletedSchemaId);
    },
  });
};
